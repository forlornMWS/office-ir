package io.officeir.word.ir;

/** 锚定图位置(简化):相对对象 + px 偏移。 */
public record WordAnchor(String relativeTo, int offsetXPx, int offsetYPx) {}
