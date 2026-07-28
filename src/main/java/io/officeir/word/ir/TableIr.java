package io.officeir.word.ir;
import java.util.List;

/** 表格块。{@code rows} 为行;cell 的 {@code blocks} 递归为 {@code List<BlockIr>}。 */
public record TableIr(List<TableRowIr> rows) implements BlockIr {}
