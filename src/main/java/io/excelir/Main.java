package io.excelir;

public class Main {
    public static void main(String[] argv) {
        try {
            CliArgs args = CliArgs.parse(argv);
            System.out.println("输入: " + args.input());
            System.out.println("输出: " + (args.output() == null ? "stdout" : args.output()));
            System.out.println("sheet: " + (args.sheet() == null ? "全部" : args.sheet()));
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            System.exit(2);
        }
    }
}
