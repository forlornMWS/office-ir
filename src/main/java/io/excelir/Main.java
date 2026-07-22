package io.excelir;

import io.excelir.extract.WorkbookExtractor;
import io.excelir.ir.IrDocument;
import io.excelir.json.JsonWriter;

import java.io.File;
import java.nio.file.Path;

public class Main {
    public static void main(String[] argv) {
        try {
            CliArgs args = CliArgs.parse(argv);
            Path mediaDir = args.output() == null
                    ? Path.of("out.media")
                    : Path.of(args.output()).resolveSibling("out.media");
            IrDocument doc = WorkbookExtractor.extract(Path.of(args.input()), args.sheet(), mediaDir);
            if (args.output() == null) {
                System.out.println(JsonWriter.write(doc));
            } else {
                JsonWriter.writeToFile(doc, new File(args.output()));
                System.err.println("已写入: " + args.output() + " (媒体目录: " + mediaDir + ")");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("错误: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("解析失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
