package io.excelir.extract;

import io.excelir.ir.MarginsIr;
import org.apache.poi.ss.usermodel.Sheet;

public final class MarginsExtractor {
    private MarginsExtractor() {}

    public static MarginsIr extract(Sheet sheet) {
        return new MarginsIr(
                sheet.getMargin(Sheet.LeftMargin),
                sheet.getMargin(Sheet.RightMargin),
                sheet.getMargin(Sheet.TopMargin),
                sheet.getMargin(Sheet.BottomMargin),
                sheet.getMargin(Sheet.HeaderMargin),
                sheet.getMargin(Sheet.FooterMargin));
    }
}
