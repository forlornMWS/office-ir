package io.officeir;

import io.officeir.excel.project.Section;
import io.officeir.word.project.WordSection;

import java.util.ArrayList;
import java.util.List;

public record CliArgs(String input, String output, Format format,
                      String sheet, String range, String select,
                      boolean pretty, boolean fullCells, boolean full) {

    public enum Format { XLSX, DOCX }

    public static CliArgs parse(String[] argv) {
        List<String> positional = new ArrayList<>();
        String output = null, sheet = null, range = null, select = null;
        boolean pretty = false, fullCells = false, full = false;
        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            switch (a) {
                case "-o", "--output" -> output = argv[++i];
                case "--sheet" -> sheet = argv[++i];
                case "--range" -> range = argv[++i];
                case "--select" -> select = argv[++i];
                case "--pretty" -> pretty = true;
                case "--full-cells" -> fullCells = true;
                case "--full" -> full = true;
                case "-h", "--help" -> { printHelp(); System.exit(0); }
                default -> positional.add(a);
            }
        }
        if (positional.isEmpty()) {
            throw new IllegalArgumentException(
                    "缺少输入文件参数。用法: office-ir <input.xlsx|input.docx> [选项]");
        }
        String input = positional.get(0);
        String low = input.toLowerCase();
        Format fmt;
        if (low.endsWith(".xlsx")) fmt = Format.XLSX;
        else if (low.endsWith(".docx")) fmt = Format.DOCX;
        else if (low.endsWith(".xls"))
            throw new IllegalArgumentException("只支持 .xlsx,收到: " + input + "。若是 .xls 请先转换为 .xlsx。");
        else if (low.endsWith(".doc"))
            throw new IllegalArgumentException("只支持 .docx,收到: " + input + "。若是 .doc 请先转换为 .docx:libreoffice --headless --convert-to docx " + input);
        else
            throw new IllegalArgumentException("不支持的输入: " + input + "(仅 .xlsx/.docx)");

        if (fmt == Format.DOCX) {
            // Word 无网格/sheet 概念,这些 flag 报错(静默忽略会误导)
            if (range != null)
                throw new IllegalArgumentException("Word 不支持 --range(Word 无文档级网格)");
            if (sheet != null)
                throw new IllegalArgumentException("Word 不支持 --sheet(Word 无 sheet)");
            if (fullCells)
                throw new IllegalArgumentException("Word 不支持 --full-cells(用 --full)");
            if (select != null) WordSection.parseAll(select); // 校验 Word 词表
        } else {
            if (range != null && range.isBlank())
                throw new IllegalArgumentException("缺少 --range 值。用法:--range A1:E6");
            if (select != null) Section.parseAll(select); // 校验 excel 词表
        }
        return new CliArgs(input, output, fmt, sheet, range, select, pretty, fullCells, full);
    }

    private static void printHelp() {
        System.out.println("用法: office-ir <input.xlsx|input.docx> [选项]");
        System.out.println("按扩展名分发:.xlsx→excel IR,.docx→Word IR");
        System.out.println("  -o, --output <file>   写入文件(默认 stdout);图片落盘到同级 out.media/");
        System.out.println("通用:--pretty(缩进)");
        System.out.println("Excel(.xlsx):");
        System.out.println("  --sheet <名称|索引>   只解析指定 sheet");
        System.out.println("  --range <A1:E6>       A1 区间裁剪(cells/merges/layout/media)");
        System.out.println("  --select <区段>       " + Section.whitelist());
        System.out.println("  --full-cells          恢复旧单元格冗余形态");
        System.out.println("Word(.docx):");
        System.out.println("  --select <区段>       " + WordSection.whitelist());
        System.out.println("  --full                不合并 run + 留空 run(调试逃逸)");
        System.out.println("默认紧凑形态(NON_NULL+run 合并/空 run 丢/空段压缩),零信息损失。");
    }
}
