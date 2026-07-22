package io.excelir.ir;
import java.util.List;
public record ChartIr(String type, AnchorIr anchor, String title, List<String> seriesRanges) {}
