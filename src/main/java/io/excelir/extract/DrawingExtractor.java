package io.excelir.extract;

import io.excelir.ir.*;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DrawingExtractor {
    private DrawingExtractor() {}

    public record DrawingResult(List<ImageIr> images, List<ChartIr> charts, List<ShapeIr> shapes) {}

    public static DrawingResult extract(XSSFSheet sheet, Path mediaDir, String sheetPrefix) {
        List<ImageIr> images = new ArrayList<>();
        List<ChartIr> charts = new ArrayList<>();
        List<ShapeIr> shapes = new ArrayList<>();
        XSSFDrawing patri = sheet.getDrawingPatriarch();
        if (patri != null) {
            int imgIdx = 0;
            for (XSSFShape shape : patri.getShapes()) {
                AnchorIr anchor = toAnchor(shape.getAnchor());
                if (shape instanceof XSSFPicture pic) {
                    images.add(toImageIr(pic, anchor, mediaDir, sheetPrefix, imgIdx++));
                } else if (shape instanceof XSSFSimpleShape ss) {
                    shapes.add(new ShapeIr(anchor, ss.getText(), sheetPrefix));
                }
            }
            for (XSSFChart chart : patri.getCharts()) {
                AnchorIr anchor = toAnchor(chart.getGraphicFrame().getAnchor());
                charts.add(toChartIr(chart, anchor));
            }
        }
        return new DrawingResult(images, charts, shapes);
    }

    private static ImageIr toImageIr(XSSFPicture pic, AnchorIr anchor, Path mediaDir, String prefix, int idx) {
        try {
            String ext = pic.getPictureData().suggestFileExtension();
            if (ext == null) ext = "png";
            Path out = mediaDir.resolve(prefix + "_img" + idx + "." + ext);
            Files.createDirectories(mediaDir);
            Files.write(out, pic.getPictureData().getData());
            java.awt.Dimension dim = pic.getImageDimension();
            return new ImageIr(out.toString(), ext,
                    anchor, dim == null ? 0 : dim.width, dim == null ? 0 : dim.height, "");
        } catch (IOException e) {
            throw new RuntimeException("图片落盘失败", e);
        }
    }

    private static ChartIr toChartIr(XSSFChart chart, AnchorIr anchor) {
        String title = chart.getTitleText() == null ? null : chart.getTitleText().getString();
        String type = "chart";
        try {
            var plotArea = chart.getCTChart().getPlotArea();
            if (plotArea.sizeOfBarChartArray() > 0) type = "bar";
            else if (plotArea.sizeOfLineChartArray() > 0) type = "line";
            else if (plotArea.sizeOfPieChartArray() > 0) type = "pie";
        } catch (Exception ignore) { /* default "chart" */ }
        return new ChartIr(type, anchor, title, List.of());
    }

    private static AnchorIr toAnchor(org.apache.poi.ss.usermodel.ChildAnchor a) {
        if (a instanceof XSSFClientAnchor ca) {
            String type = switch (ca.getAnchorType()) {
                case MOVE_DONT_RESIZE -> "oneCell";
                case DONT_MOVE_AND_RESIZE -> "absolute";
                default -> "twoCell";
            };
            return new AnchorIr(type,
                    pointFromMarker((int) ca.getCol1(), ca.getRow1(),
                            Units.emuToPx(ca.getDx1()), Units.emuToPx(ca.getDy1())),
                    pointFromMarker((int) ca.getCol2(), ca.getRow2(),
                            Units.emuToPx(ca.getDx2()), Units.emuToPx(ca.getDy2())));
        }
        return new AnchorIr("unknown", null, null);
    }

    static AnchorIr.Point pointFromMarker(int col, int row, int colOffPx, int rowOffPx) {
        return new AnchorIr.Point(col, row, colOffPx, rowOffPx);
    }
}
