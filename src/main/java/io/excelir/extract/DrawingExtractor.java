package io.excelir.extract;

import io.excelir.ir.*;
import java.util.List;

/** TEMPORARY stub — replaced by Task 12's real implementation. */
public final class DrawingExtractor {
    private DrawingExtractor() {}

    public record DrawingResult(List<ImageIr> images, List<ChartIr> charts, List<ShapeIr> shapes) {}
}
