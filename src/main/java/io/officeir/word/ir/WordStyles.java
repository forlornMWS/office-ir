package io.officeir.word.ir;
import java.util.List;

/** Word 命名样式表:段落样式 + 字符样式。 */
public record WordStyles(List<StyleIr> paragraphStyles, List<StyleIr> characterStyles) {}
