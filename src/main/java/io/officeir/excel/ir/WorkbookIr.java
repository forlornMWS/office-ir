package io.officeir.excel.ir;
import java.util.List;
public record WorkbookIr(Properties properties, List<DefinedName> definedNames, StyleTable styles) {
    public record Properties(String title, String creator, String created) {}
    public record DefinedName(String name, String ref) {}
}
