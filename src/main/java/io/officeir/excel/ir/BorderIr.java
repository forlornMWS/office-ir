package io.officeir.excel.ir;
public record BorderIr(String id, Edge top, Edge bottom, Edge left, Edge right,
                       Edge diagonalLeft, Edge diagonalRight) {
    public record Edge(String style, String color) {}
}
