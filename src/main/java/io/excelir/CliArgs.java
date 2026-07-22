package io.excelir;

import java.util.ArrayList;
import java.util.List;

public record CliArgs(String input, String output, String sheet) {

    public static CliArgs parse(String[] argv) {
        List<String> positional = new ArrayList<>();
        String output = null;
        String sheet = null;
        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            switch (a) {
                case "-o", "--output" -> output = argv[++i];
                case "--sheet" -> sheet = argv[++i];
                case "-h", "--help" -> { printHelp(); System.exit(0); }
                default -> positional.add(a);
            }
        }
        if (positional.isEmpty()) {
            throw new IllegalArgumentException("缺少输入文件参数。用法: excel-ir <input.xlsx> [-o out.json] [--sheet 名称|索引]");
        }
        String input = positional.get(0);
        if (!input.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("只支持 .xlsx,收到: " + input + "。若是 .xls 请先转换为 .xlsx。");
        }
        return new CliArgs(input, output, sheet);
    }

    private static void printHelp() {
        System.out.println("用法: excel-ir <input.xlsx> [-o out.json] [--sheet 名称|索引]");
        System.out.println("  不带 -o 时输出到 stdout;--sheet 省略时解析全部 sheet。");
    }
}
