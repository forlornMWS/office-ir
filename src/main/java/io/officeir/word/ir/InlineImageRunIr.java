package io.officeir.word.ir;

/** 行内图 run,夹在文字 run 间保流位置。{@code path} 指向落盘文件,JSON 不含像素。 */
public record InlineImageRunIr(String path, String format, int widthPx, int heightPx,
                               String alt) implements RunIr {}
