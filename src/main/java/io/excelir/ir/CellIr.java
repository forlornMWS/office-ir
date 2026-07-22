package io.excelir.ir;
public record CellIr(String ref, String type, Object raw, String formula, String formatted, String style) {}
