package io.officeir.excel.extract;

import io.officeir.excel.ir.HeaderFooterIr;
import org.apache.poi.ss.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.Sheet;

public final class HeaderFooterExtractor {
    private HeaderFooterExtractor() {}

    public static HeaderFooterIr extract(Sheet sheet) {
        HeaderFooter h = sheet.getHeader();
        HeaderFooter f = sheet.getFooter();
        HeaderFooterIr.Side oh = side(h);
        HeaderFooterIr.Side of = side(f);
        return new HeaderFooterIr(oh, of, null, null, null, null);
    }

    private static HeaderFooterIr.Side side(HeaderFooter hf) {
        if (hf == null) return null;
        String l = emptyToNull(hf.getLeft());
        String c = emptyToNull(hf.getCenter());
        String r = emptyToNull(hf.getRight());
        if (l == null && c == null && r == null) return new HeaderFooterIr.Side(null, null, null);
        return new HeaderFooterIr.Side(l, c, r);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
