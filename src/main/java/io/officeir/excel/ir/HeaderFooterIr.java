package io.officeir.excel.ir;
public record HeaderFooterIr(Side oddHeader, Side oddFooter, Side evenHeader,
                             Side evenFooter, Side firstHeader, Side firstFooter) {
    public record Side(String left, String center, String right) {}
}
