package io.officeir.excel.ir;
public record CellXfIr(String id, String font, String fill, String border,
                       String hAlign, String vAlign, boolean wrapText,
                       int textRotation, int indent, String numberFormat,
                       boolean locked, boolean hidden, boolean shrinkToFit) {}
