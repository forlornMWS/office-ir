package io.officeir.word.ir;
import java.util.List;

/** Word 文档 IR(schemaVersion 独立从 1 起)。 */
public record WordIr(int schemaVersion, WordProperties properties, WordStyles styles,
                     List<SectionIr> sections, List<WordMediaIr> media) {}
