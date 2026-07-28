package io.officeir.word.ir;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Word 正文块的密封接口。body 与 table cell 的 blocks 均为 {@code List<BlockIr>}(递归)。
 * 序列化带 {@code kind} 判别:"para"/"table"。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ParagraphIr.class, name = "para"),
        @JsonSubTypes.Type(value = TableIr.class, name = "table")
})
public sealed interface BlockIr permits ParagraphIr, TableIr {}
