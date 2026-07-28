package io.officeir.excel.ir;
import java.util.List;
public record StyleTable(List<FontIr> fonts, List<FillIr> fills, List<BorderIr> borders, List<CellXfIr> cellXfs) {}
