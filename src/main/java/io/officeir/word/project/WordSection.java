package io.officeir.word.project;

import java.util.EnumSet;
import java.util.Set;

/** Word --select 的 7 个区段词。 */
public enum WordSection {
    PARAGRAPHS, TABLES, STYLES, MEDIA, HEADERS, PAGE, PROPERTIES;

    public String cliName() { return name().toLowerCase(); }

    public static Set<WordSection> parseAll(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("缺少区段词。合法值:" + whitelist() + "(用逗号分隔多选)");
        }
        Set<WordSection> out = EnumSet.noneOf(WordSection.class);
        for (String tok : csv.split(",")) {
            String t = tok.trim();
            if (t.isEmpty()) continue;
            WordSection s;
            try { s = WordSection.valueOf(t.toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("未知区段词:" + t + "。合法值:" + whitelist());
            }
            out.add(s);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("缺少区段词。合法值:" + whitelist());
        return out;
    }

    public static String whitelist() {
        StringBuilder sb = new StringBuilder();
        for (WordSection s : values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.cliName());
        }
        return sb.toString();
    }
}
