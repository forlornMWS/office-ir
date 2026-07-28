package io.officeir.word.ir;
import java.util.List;

/** Word 节。持有 pageSetup + 页眉页脚 + body 块流。 */
public record SectionIr(WordPageSetupIr pageSetup, HeaderFooters headers,
                       List<BlockIr> body) {}
