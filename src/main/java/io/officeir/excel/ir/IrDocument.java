package io.officeir.excel.ir;
import java.util.List;
public record IrDocument(int schemaVersion, WorkbookIr workbook, List<SheetIr> sheets) {}
