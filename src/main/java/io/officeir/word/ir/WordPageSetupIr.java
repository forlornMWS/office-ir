package io.officeir.word.ir;

/** Word 节页面布局。margins 单位英寸。 */
public record WordPageSetupIr(String paperSize, String orientation, WordMargins margins) {}
