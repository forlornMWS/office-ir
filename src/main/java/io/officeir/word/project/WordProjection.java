package io.officeir.word.project;

import io.officeir.word.ir.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Word 纯数据投影:输入 WordIr + select,输出裁剪后 WordIr(同类型)。
 * 规则(D9/D10/D15):
 *   - paragraphs 递归穿透表 cell(仅当 tables 未选时);tables 整表留含内嵌。
 *   - 选正文块(paragraphs/tables/headers)闭包带引用样式子集;显式 select styles 取全表。
 *   - 纯全局区段(styles/media/page/properties)无空间归属。
 * 无 range 维度(Word 无文档级网格)。
 */
public final class WordProjection {
    private WordProjection() {}

    public static WordIr project(WordIr doc, Set<WordSection> select) {
        Set<WordSection> sel = (select == null) ? EnumSet.allOf(WordSection.class) : select;

        WordProperties props = sel.contains(WordSection.PROPERTIES) ? doc.properties() : null;

        // styles:显式选 → 全表;选正文块 → 闭包子集(v1 样式表为空,闭包无操作);否则省略
        WordStyles styles;
        boolean bodySelected = sel.contains(WordSection.PARAGRAPHS) || sel.contains(WordSection.TABLES)
                || sel.contains(WordSection.HEADERS);
        if (sel.contains(WordSection.STYLES)) {
            styles = doc.styles();
        } else if (bodySelected) {
            styles = closureStyles(doc); // v1 样式表为空 → 空;留闭包框架
        } else {
            styles = null;
        }

        boolean paras = sel.contains(WordSection.PARAGRAPHS);
        boolean tabs = sel.contains(WordSection.TABLES);
        List<SectionIr> sectionsOut = new ArrayList<>();
        for (SectionIr s : doc.sections()) {
            List<BlockIr> body = filterBody(s.body(), paras, tabs);
            WordPageSetupIr ps = sel.contains(WordSection.PAGE) ? s.pageSetup() : null;
            HeaderFooters hf = sel.contains(WordSection.HEADERS) ? s.headers() : null;
            sectionsOut.add(new SectionIr(ps, hf, body));
        }

        List<WordMediaIr> media = sel.contains(WordSection.MEDIA) ? doc.media() : null;
        return new WordIr(doc.schemaVersion(), props, styles, sectionsOut,
                media == null ? List.of() : media);
    }

    /** paragraphs/tables 过滤:仅 paragraphs(穿透表 cell,当 tables 未选) / 仅 tables(整表) / 二者。 */
    private static List<BlockIr> filterBody(List<BlockIr> body, boolean paras, boolean tabs) {
        if (!paras && !tabs) return null;
        List<BlockIr> out = new ArrayList<>();
        for (BlockIr b : body) {
            if (b instanceof ParagraphIr p) {
                if (paras) out.add(p);
            } else if (b instanceof TableIr t) {
                if (tabs) {
                    out.add(t); // 整表留含内嵌
                } else if (paras) {
                    // tables 未选但 paragraphs 选 → 穿透表 cell 取其中段落
                    collectParasFromTable(t, out);
                }
            }
        }
        return out;
    }

    private static void collectParasFromTable(TableIr t, List<BlockIr> sink) {
        for (TableRowIr row : t.rows()) {
            for (TableCellIr cell : row.cells()) {
                if (cell.blocks() == null) continue;
                for (BlockIr b : cell.blocks()) {
                    if (b instanceof ParagraphIr p) sink.add(p);
                    else if (b instanceof TableIr nested) collectParasFromTable(nested, sink);
                }
            }
        }
    }

    /** v1:样式表为空,闭包无操作;留框架供 v2 实现引用解析。 */
    private static WordStyles closureStyles(WordIr doc) {
        return doc.styles(); // 空表,原样返回
    }
}
