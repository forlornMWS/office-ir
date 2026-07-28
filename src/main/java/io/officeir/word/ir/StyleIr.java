package io.officeir.word.ir;

/** 命名样式(段落/字符)。{@code basedOn} 指向父样式 id;{@code run}/{@code para} 为解析继承后的有效值。 */
public record StyleIr(String id, String name, String basedOn, RunFormat run,
                      ParaFormat para, Integer headingLevel) {}
