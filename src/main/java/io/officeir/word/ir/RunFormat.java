package io.officeir.word.ir;

/** run 级直接格式覆盖(稀疏,各字段可空,NON_NULL 省略)。命名样式与 run 覆盖共用此结构。 */
public record RunFormat(String font, Double sizePt, Boolean bold, Boolean italic,
                        String underline, String color, Boolean strike, String vertAlign) {}
