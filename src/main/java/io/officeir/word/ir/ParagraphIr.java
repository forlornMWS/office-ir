package io.officeir.word.ir;
import java.util.List;

/** 段落块。{@code style} 命名段落样式 ref(可空),{@code numbering} 编号(可空),
 *  {@code para} 段落格式覆盖(可空),{@code runs} 为 run 流(含行内图,保位置)。 */
public record ParagraphIr(String style, NumberingIr numbering, ParaFormat para,
                         List<RunIr> runs) implements BlockIr {}
