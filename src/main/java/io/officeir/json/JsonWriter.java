package io.officeir.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.officeir.excel.ir.CellIr;
import io.officeir.excel.ir.ColumnIr;
import io.officeir.excel.ir.HeaderFooterIr;
import io.officeir.excel.ir.IrDocument;
import io.officeir.word.ir.WordIr;
import java.io.File;
import java.io.IOException;

/**
 * IR 序列化。默认紧凑形态:单行 + NON_NULL + 空单元格压缩 + formatted==raw 省略 + widthChars 省略 +
 * 全 null headerFooter 块省略。{@code --full-cells} 恢复单元格冗余形态;{@code --pretty} 恢复缩进。
 * 两者正交,且均不恢复显式 null(NON_NULL 固定)。
 */
public class JsonWriter {

    public static String write(IrDocument doc) {
        return write(doc, false, false);
    }

    public static String write(IrDocument doc, boolean pretty, boolean fullCells) {
        try {
            return mapper(pretty, fullCells).writeValueAsString(doc);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public static void writeToFile(IrDocument doc, File file) {
        writeToFile(doc, file, false, false);
    }

    public static void writeToFile(IrDocument doc, File file, boolean pretty, boolean fullCells) {
        try {
            mapper(pretty, fullCells).writeValue(file, doc);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    // ---------- Word ----------

    /** Word 序列化:NON_NULL + 可选缩进。BlockIr/RunIr 多态判别由各自 @JsonTypeInfo 处理;
     *  run 合并/空 run 丢弃/空段压缩在提取层完成(受 --full 控制)。 */
    public static String write(WordIr doc, boolean pretty) {
        try {
            return wordMapper(pretty).writeValueAsString(doc);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public static void writeToFile(WordIr doc, File file, boolean pretty) {
        try {
            wordMapper(pretty).writeValue(file, doc);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    private static ObjectMapper wordMapper(boolean pretty) {
        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (pretty) m.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        return m;
    }

    private static ObjectMapper mapper(boolean pretty, boolean fullCells) {
        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (pretty) m.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        SimpleModule mod = new SimpleModule("excel-ir");
        mod.addSerializer(CellIr.class, new CellIrSerializer(fullCells));
        mod.addSerializer(ColumnIr.class, new ColumnIrSerializer());
        mod.addSerializer(HeaderFooterIr.class, new HeaderFooterIrSerializer());
        m.registerModule(mod);
        return m;
    }

    /** 单元格:blank→{ref,style};非 blank→省略 formatted==raw、formula==null。fullCells 恢复冗余形态。 */
    static final class CellIrSerializer extends JsonSerializer<CellIr> {
        private final boolean fullCells;
        CellIrSerializer(boolean fullCells) { this.fullCells = fullCells; }

        @Override
        public void serialize(CellIr c, JsonGenerator g, SerializerProvider p) throws IOException {
            g.writeStartObject();
            g.writeStringField("ref", c.ref());
            boolean blank = "blank".equals(c.type());
            if (!fullCells && blank) {
                g.writeStringField("style", c.style());
            } else {
                g.writeStringField("type", c.type());
                if (c.raw() != null) { g.writeFieldName("raw"); g.writeObject(c.raw()); }
                if (c.formula() != null) g.writeStringField("formula", c.formula());
                boolean formattedRedundant = !fullCells
                        && (c.raw() instanceof String s) && s.equals(c.formatted());
                if (c.formatted() != null && !formattedRedundant) g.writeStringField("formatted", c.formatted());
                g.writeStringField("style", c.style());
            }
            g.writeEndObject();
        }
    }

    /** 列宽:固定省略可推导的 widthChars,仅保留 idx/widthPx/hidden(D8:固定默认行为)。 */
    static final class ColumnIrSerializer extends JsonSerializer<ColumnIr> {
        @Override
        public void serialize(ColumnIr c, JsonGenerator g, SerializerProvider p) throws IOException {
            g.writeStartObject();
            g.writeNumberField("idx", c.idx());
            g.writeNumberField("widthPx", c.widthPx());
            g.writeBooleanField("hidden", c.hidden());
            g.writeEndObject();
        }
    }

    /** 页眉页脚:全空则整体省略(NON_EMPTY + isEmpty);非空只写非空 side。 */
    static final class HeaderFooterIrSerializer extends JsonSerializer<HeaderFooterIr> {
        @Override
        public boolean isEmpty(SerializerProvider provider, HeaderFooterIr hf) {
            return hf == null || (isEmpty(hf.oddHeader()) && isEmpty(hf.oddFooter())
                    && isEmpty(hf.evenHeader()) && isEmpty(hf.evenFooter())
                    && isEmpty(hf.firstHeader()) && isEmpty(hf.firstFooter()));
        }

        @Override
        public void serialize(HeaderFooterIr hf, JsonGenerator g, SerializerProvider p) throws IOException {
            g.writeStartObject();
            writeSide(g, "oddHeader", hf.oddHeader());
            writeSide(g, "oddFooter", hf.oddFooter());
            writeSide(g, "evenHeader", hf.evenHeader());
            writeSide(g, "evenFooter", hf.evenFooter());
            writeSide(g, "firstHeader", hf.firstHeader());
            writeSide(g, "firstFooter", hf.firstFooter());
            g.writeEndObject();
        }

        private static boolean isEmpty(HeaderFooterIr.Side s) {
            return s == null || (s.left() == null && s.center() == null && s.right() == null);
        }

        private static void writeSide(JsonGenerator g, String name, HeaderFooterIr.Side s) throws IOException {
            if (isEmpty(s)) return;
            g.writeObjectFieldStart(name);
            if (s.left() != null) g.writeStringField("left", s.left());
            if (s.center() != null) g.writeStringField("center", s.center());
            if (s.right() != null) g.writeStringField("right", s.right());
            g.writeEndObject();
        }
    }
}
