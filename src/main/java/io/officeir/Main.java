package io.officeir;

import io.officeir.excel.extract.WorkbookExtractor;
import io.officeir.excel.ir.IrDocument;
import io.officeir.excel.project.Projection;
import io.officeir.excel.project.Range;
import io.officeir.excel.project.Section;
import io.officeir.json.JsonWriter;
import io.officeir.word.extract.WordDocumentExtractor;
import io.officeir.word.ir.WordIr;
import io.officeir.word.project.WordProjection;
import io.officeir.word.project.WordSection;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

public class Main {
    public static void main(String[] argv) {
        try {
            CliArgs args = CliArgs.parse(argv);
            Path mediaDir = args.output() == null
                    ? Path.of("out.media")
                    : Path.of(args.output()).resolveSibling("out.media");

            if (args.format() == CliArgs.Format.DOCX) {
                runWord(args, mediaDir);
            } else {
                runExcel(args, mediaDir);
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

    private static void runWord(CliArgs args, Path mediaDir) throws Exception {
        WordIr doc = WordDocumentExtractor.extract(Path.of(args.input()), mediaDir, args.full());
        Set<WordSection> select = args.select() == null ? null : WordSection.parseAll(args.select());
        WordIr projected = WordProjection.project(doc, select);
        if (args.output() == null) {
            System.out.println(JsonWriter.write(projected, args.pretty()));
        } else {
            JsonWriter.writeToFile(projected, new File(args.output()), args.pretty());
            System.err.println("已写入: " + args.output() + " (媒体目录: " + mediaDir + ")");
        }
    }

    private static void runExcel(CliArgs args, Path mediaDir) throws Exception {
        IrDocument doc = WorkbookExtractor.extract(Path.of(args.input()), args.sheet(), mediaDir);
        Range range = args.range() == null ? null : Range.parse(args.range());
        Set<Section> select = args.select() == null ? null : Section.parseAll(args.select());
        IrDocument projected = Projection.project(doc, range, select);
        if (args.output() == null) {
            System.out.println(JsonWriter.write(projected, args.pretty(), args.fullCells()));
        } else {
            JsonWriter.writeToFile(projected, new File(args.output()), args.pretty(), args.fullCells());
            System.err.println("已写入: " + args.output() + " (媒体目录: " + mediaDir + ")");
        }
    }
}
