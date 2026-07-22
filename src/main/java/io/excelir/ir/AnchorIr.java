package io.excelir.ir;
public record AnchorIr(String type, Point from, Point to) {
    public record Point(int col, int row, int colOffPx, int rowOffPx) {}
}
