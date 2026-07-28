package io.officeir.word.ir;

/** 段落编号信息。{@code kind} 为可读枚举:bullet/decimal/lowerLetter/upperLetter/lowerRoman/upperRoman/... */
public record NumberingIr(int numId, int ilvl, String kind) {}
