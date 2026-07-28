package io.officeir.word.ir;

/** 段落级格式覆盖(稀疏,各字段可空)。 */
public record ParaFormat(String align, Double spacingBeforePt, Double spacingAfterPt,
                         Double lineSpacingPt, Double indentLeftPt, Double indentRightPt) {}
