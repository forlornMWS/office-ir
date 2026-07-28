package io.officeir.word.extract;

import io.officeir.word.ir.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * .docx → WordIr(高层 XWPF + 必要 CT XMLBeans)。
 *
 * v2 覆盖(相对 v1 精化):
 *   - 命名样式表:遍历 styles.xml(CTStyles.getStyleList),读 id/name/basedOn/type +
 *     run 格式(font/size/bold/italic/color/strike),解析 basedOn 继承到有效值。
 *   - 编号 kind:numId→abstractNum→lvl(ilvl)→numFmt,映射可读枚举(bullet/decimal/...)。
 *   - 单节 pageSetup:body.sectPr 的 pgSz(纸张/方向)+ pgMar(页边距,英寸)。
 * v1 已覆盖(保留):文档属性、body 块流(段落/表格,递归)、run(文本+行内图,D3 合并)、
 *   编号(numId/ilvl)、行内图落盘。
 *
 * 仍简化(留后续):多节(只取文档末 sectPr)、页眉页脚内容、表格 gridSpan/vMerge/shading
 *   (CTTcPr 在本 POI 构建无对应 getter,需 XmlCursor,留后续)、锚定图。
 */
public final class WordDocumentExtractor {
    private WordDocumentExtractor() {}

    public static WordIr extract(Path docx, Path mediaDir, boolean full) throws Exception {
        try (var wb = new XWPFDocument(new FileInputStream(docx.toFile()))) {
            return run(wb, mediaDir, full);
        }
    }

    private static WordIr run(XWPFDocument doc, Path mediaDir, boolean full) throws Exception {
        WordProperties props = properties(doc);
        WordStyles styles = styles(doc);
        List<BlockIr> body = bodyBlocks(doc, mediaDir, full);
        WordPageSetupIr ps = pageSetup(doc);
        HeaderFooters hf = null; // 页眉页脚内容留后续
        SectionIr section = new SectionIr(ps, hf, body);
        return new WordIr(1, props, styles, List.of(section), List.of());
    }

    // ---------- properties ----------

    private static WordProperties properties(XWPFDocument doc) {
        var cp = doc.getProperties().getCoreProperties();
        return new WordProperties(cp.getTitle(), cp.getCreator(),
                cp.getCreated() == null ? null : cp.getCreated().toString());
    }

    // ---------- 命名样式表(v2) ----------

    private static WordStyles styles(XWPFDocument doc) throws Exception {
        List<StyleIr> para = new ArrayList<>();
        List<StyleIr> chr = new ArrayList<>();
        Map<String, StyleIr> byId = new HashMap<>();
        CTStyles ct = ctStylesOf(doc.getStyles()); // POI 已解析的内部 CTStyles(反射)
        if (ct != null) {
            for (CTStyle s : ct.getStyleList()) {
                String id = s.isSetStyleId() ? s.getStyleId() : null;
                String name = s.isSetName() ? s.getName().getVal() : null;
                String basedOn = s.isSetBasedOn() ? s.getBasedOn().getVal() : null;
                RunFormat rf = runFormatOf(s);
                boolean isChar = s.isSetType()
                        && s.getType() == STStyleType.CHARACTER;
                StyleIr ir = new StyleIr(id, name, basedOn, rf, null, null);
                if (isChar) chr.add(ir); else para.add(ir);
                if (id != null) byId.put(id, ir);
            }
        }
        resolveInheritance(para, byId);
        resolveInheritance(chr, byId);
        return new WordStyles(para, chr);
    }

    /** 反射 XWPFStyles 的私有 ctStyles 字段(POI 已解析 styles.xml);公开 API 无枚举入口。 */
    private static CTStyles ctStylesOf(XWPFStyles xs) {
        if (xs == null) return null;
        try {
            var f = xs.getClass().getDeclaredField("ctStyles");
            f.setAccessible(true);
            return (CTStyles) f.get(xs);
        } catch (Throwable t) {
            return null;
        }
    }

    private static RunFormat runFormatOf(CTStyle s) {
        if (!s.isSetRPr()) return null;
        CTRPr r = s.getRPr();
        String font = r.sizeOfRFontsArray() > 0 && r.getRFontsArray(0).isSetAscii()
                ? r.getRFontsArray(0).getAscii() : null;
        Double size = r.sizeOfSzArray() > 0
                ? Double.parseDouble(r.getSzArray(0).xgetVal().getStringValue().trim()) / 2.0 : null;
        Boolean bold = r.sizeOfBArray() > 0 ? onOff(r.getBArray(0)) : null;
        Boolean italic = r.sizeOfIArray() > 0 ? onOff(r.getIArray(0)) : null;
        String color = r.sizeOfColorArray() > 0 ? r.getColorArray(0).xgetVal().getStringValue() : null;
        if ("auto".equalsIgnoreCase(color)) color = null;
        Boolean strike = r.sizeOfStrikeArray() > 0 ? onOff(r.getStrikeArray(0)) : null;
        if (font == null && size == null && bold == null && italic == null && color == null && strike == null)
            return null;
        return new RunFormat(font, size, bold, italic, null, color, strike, null);
    }

    private static Boolean onOff(CTOnOff o) {
        if (!o.isSetVal()) return Boolean.TRUE;
        String v = o.xgetVal().getStringValue().toLowerCase();
        return !("false".equals(v) || "0".equals(v) || "off".equals(v));
    }

    /** 解析 basedOn 继承:子覆盖父,深度上限+防环。 */
    private static void resolveInheritance(List<StyleIr> list, Map<String, StyleIr> byId) {
        for (int i = 0; i < list.size(); i++) {
            StyleIr child = list.get(i);
            RunFormat r = child.run();
            String basedOn = child.basedOn();
            int depth = 0;
            java.util.Set<String> seen = new java.util.HashSet<>();
            while (basedOn != null && depth < 8 && seen.add(basedOn)) {
                StyleIr parent = byId.get(basedOn);
                if (parent == null) break;
                r = mergeRun(parent.run(), r);
                basedOn = parent.basedOn();
                depth++;
            }
            list.set(i, new StyleIr(child.id(), child.name(), child.basedOn(),
                    r, child.para(), child.headingLevel()));
        }
    }

    private static RunFormat mergeRun(RunFormat parent, RunFormat child) {
        if (parent == null) return child;
        if (child == null) return parent;
        return new RunFormat(
                child.font() != null ? child.font() : parent.font(),
                child.sizePt() != null ? child.sizePt() : parent.sizePt(),
                child.bold() != null ? child.bold() : parent.bold(),
                child.italic() != null ? child.italic() : parent.italic(),
                child.underline() != null ? child.underline() : parent.underline(),
                child.color() != null ? child.color() : parent.color(),
                child.strike() != null ? child.strike() : parent.strike(),
                child.vertAlign() != null ? child.vertAlign() : parent.vertAlign());
    }

    // ---------- 编号 kind(v2) ----------

    private static NumberingIr numbering(XWPFParagraph p) {
        BigInteger numId = p.getNumID();
        if (numId == null) return null;
        BigInteger ilvl = p.getNumIlvl();
        int il = ilvl == null ? 0 : ilvl.intValue();
        String kind = "decimal";
        try {
            XWPFNumbering n = p.getDocument().getNumbering();
            if (n != null) {
                BigInteger absId = n.getAbstractNumID(numId);
                if (absId != null) {
                    XWPFAbstractNum an = n.getAbstractNum(absId);
                    if (an != null) {
                        for (CTLvl lvl : an.getCTAbstractNum().getLvlList()) {
                            if (lvl.getIlvl() != null && lvl.getIlvl().intValue() == il && lvl.isSetNumFmt()) {
                                kind = numFmtName(lvl.getNumFmt().xgetVal().getStringValue());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignore) { /* 降级 decimal */ }
        return new NumberingIr(numId.intValue(), il, kind);
    }

    private static String numFmtName(String ooxml) {
        if (ooxml == null) return "decimal";
        return switch (ooxml) {
            case "bullet" -> "bullet";
            case "decimal" -> "decimal";
            case "lowerLetter" -> "lowerLetter";
            case "upperLetter" -> "upperLetter";
            case "lowerRoman" -> "lowerRoman";
            case "upperRoman" -> "upperRoman";
            case "decimalZero" -> "decimalZero";
            default -> ooxml;
        };
    }

    // ---------- pageSetup(v2:单节 sectPr) ----------

    private static WordPageSetupIr pageSetup(XWPFDocument doc) {
        CTBody body = doc.getDocument().isSetBody() ? doc.getDocument().getBody() : null;
        if (body == null || !body.isSetSectPr()) return null;
        CTSectPr sp = body.getSectPr();
        String paper = null, orient = null;
        if (sp.isSetPgSz()) {
            CTPageSz sz = sp.getPgSz();
            long w = sz.isSetW() ? Long.parseLong(sz.xgetW().getStringValue().trim()) : 0;
            long h = sz.isSetH() ? Long.parseLong(sz.xgetH().getStringValue().trim()) : 0;
            paper = paperName(w, h);
            orient = (w != 0 && h != 0 && w > h) ? "landscape" : "portrait";
        }
        WordMargins mg = margins(sp);
        return new WordPageSetupIr(paper, orient, mg);
    }

    private static String paperName(long wTwips, long hTwips) {
        long wmm = Math.round(wTwips / 56.7);
        long hmm = Math.round(hTwips / 56.7);
        if ((wmm >= 205 && wmm <= 215) && (hmm >= 290 && hmm <= 300)) return "A4";
        return wmm + "x" + hmm + "mm";
    }

    private static WordMargins margins(CTSectPr sp) {
        if (!sp.isSetPgMar()) return null;
        CTPageMar m = sp.getPgMar();
        return new WordMargins(
                m.getTop() != null ? Long.parseLong(m.xgetTop().getStringValue().trim()) / 1440.0 : null,
                m.getBottom() != null ? Long.parseLong(m.xgetBottom().getStringValue().trim()) / 1440.0 : null,
                m.getLeft() != null ? Long.parseLong(m.xgetLeft().getStringValue().trim()) / 1440.0 : null,
                m.getRight() != null ? Long.parseLong(m.xgetRight().getStringValue().trim()) / 1440.0 : null,
                m.getHeader() != null ? Long.parseLong(m.xgetHeader().getStringValue().trim()) / 1440.0 : null,
                m.getFooter() != null ? Long.parseLong(m.xgetFooter().getStringValue().trim()) / 1440.0 : null);
    }

    // ---------- body 块流(递归) ----------

    private static List<BlockIr> bodyBlocks(IBody body, Path mediaDir, boolean full) {
        List<BlockIr> out = new ArrayList<>();
        for (IBodyElement el : body.getBodyElements()) {
            BlockIr b = blockOf(el, mediaDir, full);
            if (b != null) out.add(b);
        }
        return out;
    }

    private static BlockIr blockOf(IBodyElement el, Path mediaDir, boolean full) {
        if (el instanceof XWPFParagraph p) return paragraph(p, mediaDir, full);
        if (el instanceof XWPFTable t) return table(t, mediaDir, full);
        return null;
    }

    private static ParagraphIr paragraph(XWPFParagraph p, Path mediaDir, boolean full) {
        String style = p.getStyle();
        NumberingIr num = numbering(p);
        ParaFormat pf = new ParaFormat(
                p.getAlignment() != null ? p.getAlignment().name().toLowerCase() : null,
                p.getSpacingBefore() > 0 ? (double) p.getSpacingBefore() / 20.0 : null,
                p.getSpacingAfter() > 0 ? (double) p.getSpacingAfter() / 20.0 : null,
                null,
                p.getIndentFromLeft() > 0 ? (double) p.getIndentFromLeft() / 20.0 : null,
                null);
        List<RunIr> runs = runs(p, mediaDir, full);
        return new ParagraphIr(style, num, pf, runs);
    }

    // ---------- runs(含行内图)+ D3 合并 ----------

    private static List<RunIr> runs(XWPFParagraph p, Path mediaDir, boolean full) {
        List<RunIr> raw = new ArrayList<>();
        int[] imgIdx = {0};
        for (XWPFRun r : p.getRuns()) {
            for (var pic : r.getEmbeddedPictures()) {
                String path = dumpPicture(pic, mediaDir, imgIdx);
                raw.add(new InlineImageRunIr(path, extOf(pic), 0, 0, ""));
            }
            String text = r.text();
            boolean hasImage = !r.getEmbeddedPictures().isEmpty();
            if ((text == null || text.isEmpty()) && !hasImage) {
                if (full) raw.add(new TextRunIr("", r.getStyle(), runOverrides(r)));
                continue;
            }
            raw.add(new TextRunIr(text == null ? "" : text, r.getStyle(), runOverrides(r)));
        }
        if (full) return raw;
        List<RunIr> c = collapse(raw);
        return c.isEmpty() ? null : c;
    }

    private static RunFormat runOverrides(XWPFRun r) {
        String font = r.getFontName();
        Double size = r.getFontSize() > 0 ? (double) r.getFontSize() : null;
        Boolean bold = r.isBold() ? Boolean.TRUE : null;
        Boolean italic = r.isItalic() ? Boolean.TRUE : null;
        String color = r.getColor() != null && !r.getColor().isEmpty() ? r.getColor() : null;
        String underline = null;
        try {
            Object u = r.getUnderline();
            if (u != null) {
                String un = u.toString().toLowerCase();
                if (!un.contains("none")) underline = un;
            }
        } catch (Throwable ignore) { /* best-effort */ }
        Boolean strike = r.isStrikeThrough() ? Boolean.TRUE : null;
        String vertAlign = null;
        if (font == null && size == null && bold == null && italic == null && color == null
                && underline == null && strike == null && vertAlign == null) return null;
        return new RunFormat(font, size, bold, italic, underline, color, strike, vertAlign);
    }

    private static List<RunIr> collapse(List<RunIr> in) {
        List<RunIr> out = new ArrayList<>();
        for (RunIr r : in) {
            if (!(r instanceof TextRunIr cur)) { out.add(r); continue; }
            if (!out.isEmpty() && out.get(out.size() - 1) instanceof TextRunIr prev
                    && sameOverrides(prev.overrides(), cur.overrides())
                    && eq(prev.style(), cur.style())) {
                out.set(out.size() - 1, new TextRunIr(prev.text() + cur.text(),
                        cur.style(), cur.overrides()));
            } else {
                out.add(cur);
            }
        }
        return out;
    }

    private static boolean sameOverrides(RunFormat a, RunFormat b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return eq(a.font(), b.font()) && eq(a.sizePt(), b.sizePt())
                && eq(a.bold(), b.bold()) && eq(a.italic(), b.italic())
                && eq(a.underline(), b.underline()) && eq(a.color(), b.color())
                && eq(a.strike(), b.strike()) && eq(a.vertAlign(), b.vertAlign());
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // ---------- tables(递归,cell 留 blocks) ----------

    private static TableIr table(XWPFTable t, Path mediaDir, boolean full) {
        List<TableRowIr> rows = new ArrayList<>();
        for (XWPFTableRow r : t.getRows()) {
            List<TableCellIr> cells = new ArrayList<>();
            for (XWPFTableCell c : r.getTableCells()) cells.add(cell(c, mediaDir, full));
            rows.add(new TableRowIr(cells));
        }
        return new TableIr(rows);
    }

    private static TableCellIr cell(XWPFTableCell c, Path mediaDir, boolean full) {
        List<BlockIr> blocks = new ArrayList<>();
        for (IBodyElement el : c.getBodyElements()) {
            BlockIr b = blockOf(el, mediaDir, full);
            if (b != null) blocks.add(b);
        }
        // gridSpan/vMerge/shading:CTTcPr 在本 POI 构建无 getter,留后续(XmlCursor)
        return new TableCellIr(blocks, null, null, null, null, null);
    }

    // ---------- 图片落盘 ----------

    private static String dumpPicture(XWPFPicture pic, Path mediaDir, int[] idx) {
        try {
            var pd = pic.getPictureData();
            String ext = pd.suggestFileExtension();
            if (ext == null) ext = "png";
            Files.createDirectories(mediaDir);
            Path out = mediaDir.resolve("word_img" + (idx[0]++) + "." + ext);
            Files.write(out, pd.getData());
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String extOf(XWPFPicture pic) {
        try { return pic.getPictureData().suggestFileExtension(); }
        catch (Exception e) { return "png"; }
    }
}
