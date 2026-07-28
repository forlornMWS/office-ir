package io.officeir.word.ir;
import java.util.List;

/** 节的页眉页脚(奇/偶/首页 × 页眉/页脚),各为 {@code List<BlockIr>} 块流,未定义则省略。 */
public record HeaderFooters(List<BlockIr> oddHeader, List<BlockIr> oddFooter,
                            List<BlockIr> evenHeader, List<BlockIr> evenFooter,
                            List<BlockIr> firstHeader, List<BlockIr> firstFooter) {}
