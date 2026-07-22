package io.excelir.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.excelir.ir.IrDocument;
import java.io.File;
import java.io.IOException;

public class JsonWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static String write(IrDocument doc) {
        try {
            return MAPPER.writeValueAsString(doc);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public static void writeToFile(IrDocument doc, File file) {
        try {
            MAPPER.writeValue(file, doc);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }
}
