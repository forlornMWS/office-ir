package io.officeir.word.ir;
import java.util.List;

/** 表格单元。{@code colSpan} 跨列数(默认1省略);{@code vMerge} "restart"(起)/"continue"(续上格),无纵向合并省略;
 *  {@code widthPx}/{@code shading}/{@code border} 为布局+格式覆盖。{@code blocks} 递归块流。 */
public record TableCellIr(List<BlockIr> blocks, Integer colSpan, String vMerge,
                          Integer widthPx, String shading, CellBorder border) {}
