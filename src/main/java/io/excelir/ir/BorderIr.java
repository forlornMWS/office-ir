package io.excelir.ir;
public record BorderIr(String id, Edge top, Edge bottom, Edge left, Edge right) {
    public record Edge(String style, String color) {}
}
