package io.officeir.excel.project;

import java.util.EnumSet;
import java.util.Set;

/** --select 的 9 个区段词(layout/page/media 为聚合组)。严格投影:只返命名区段。 */
public enum Section {
    CELLS, MERGES, BORDERS, FONTS, FILLS, XFS, LAYOUT, PAGE, MEDIA;

    public String cliName() {
        return name().toLowerCase();
    }

    /** 解析逗号分隔的区段词;未知词抛 IAE(Main 映射为 exit 2 + 词表清单)。 */
    public static Set<Section> parseAll(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException(
                    "缺少区段词。合法值:" + whitelist() + "(用逗号分隔多选)");
        }
        Set<Section> out = EnumSet.noneOf(Section.class);
        for (String tok : csv.split(",")) {
            String t = tok.trim();
            if (t.isEmpty()) continue;
            Section s;
            try {
                s = Section.valueOf(t.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "未知区段词:" + t + "。合法值:" + whitelist());
            }
            out.add(s);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("缺少区段词。合法值:" + whitelist());
        }
        return out;
    }

    public static String whitelist() {
        StringBuilder sb = new StringBuilder();
        for (Section s : values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.cliName());
        }
        return sb.toString();
    }
}
