package io.officeir.word.ir;

/** 锚定图(浮动)。{@code path} 落盘引用,JSON 不含像素。 */
public record WordMediaIr(String path, String format, int widthPx, int heightPx, WordAnchor anchor) {}
