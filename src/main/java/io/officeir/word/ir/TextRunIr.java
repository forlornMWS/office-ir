package io.officeir.word.ir;

/** 文本 run。{@code style} 为命名字符样式 ref(可空),{@code overrides} 为稀疏直接覆盖(可空)。 */
public record TextRunIr(String text, String style, RunFormat overrides) implements RunIr {}
