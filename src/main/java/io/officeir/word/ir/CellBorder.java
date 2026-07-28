package io.officeir.word.ir;

public record CellBorder(Edge top, Edge bottom, Edge left, Edge right) {
    public record Edge(String style, String color) {}
}
