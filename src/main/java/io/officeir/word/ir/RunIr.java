package io.officeir.word.ir;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 段落内 run 的密封接口。序列化带 {@code kind} 判别:"text"/"image"。
 * 行内图作为 image run 夹在文字 run 间,保流位置。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextRunIr.class, name = "text"),
        @JsonSubTypes.Type(value = InlineImageRunIr.class, name = "image")
})
public sealed interface RunIr permits TextRunIr, InlineImageRunIr {}
