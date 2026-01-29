package net.paulem.krimson.resourcepack;

import net.paulem.krimson.common.KrimsonPlugin;
import net.radstevee.packed.core.pack.PackFormat;
import java.lang.reflect.Field;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicPackResolver {
    private DynamicPackResolver() {
        // Utility class
    }

    private static final TreeMap<MinecraftVersion, Integer> VERSION_MAP = new TreeMap<>();
    // Regex pour capturer le premier groupe de chiffres (ex: 1_21_4)
    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d+)_(\\d+)(?:_(\\d+))?");

    static {
        try {
            Field[] fields = PackFormat.class.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                // Traiter uniquement les champs qui commencent par "V"
                if (fieldName.startsWith("V")) {
                    Matcher matcher = VERSION_PATTERN.matcher(fieldName);
                    if (matcher.find()) {
                        try {
                            int fieldValue = field.getInt(null);

                            int major = Integer.parseInt(matcher.group(1));
                            int minor = Integer.parseInt(matcher.group(2));
                            int patch = (matcher.group(3) != null) ? Integer.parseInt(matcher.group(3)) : 0;

                            VERSION_MAP.put(new MinecraftVersion(major, minor, patch), fieldValue);
                        } catch (IllegalAccessException e) {
                            KrimsonPlugin.getInstance().getLogger().severe("Error accessing PackFormat field: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().severe("Error loading PackFormat fields via reflection: " + e.getMessage());
        }
    }

    /**
     * @param versionStr La version client (ex: "1.21.3")
     * @return Le PackFormat correspondant ou le plus proche en dessous
     */
    public static Integer getFromVersionName(String versionStr) {
        MinecraftVersion inputVersion = MinecraftVersion.parse(versionStr);
        var entry = VERSION_MAP.floorEntry(inputVersion);
        return (entry != null) ? entry.getValue() : PackFormat.LATEST;
    }

    // Classe de comparaison de version
    private record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {
        static MinecraftVersion parse(String v) {
            // remove any suffixes like "-SNAPSHOT"
            int suffixIndex = v.indexOf('-');
            if (suffixIndex != -1) {
                v = v.substring(0, suffixIndex);
            }

            KrimsonPlugin.getInstance().getLogger().info("Parsing version: %s".formatted(v));
            String[] parts = v.split("\\.");
            return new MinecraftVersion(
                    parts.length > 0 ? Integer.parseInt(parts[0]) : 0,
                    parts.length > 1 ? Integer.parseInt(parts[1]) : 0,
                    parts.length > 2 ? Integer.parseInt(parts[2]) : 0
            );
        }

        @Override
        public int compareTo(MinecraftVersion o) {
            if (this.major != o.major) return Integer.compare(this.major, o.major);
            if (this.minor != o.minor) return Integer.compare(this.minor, o.minor);
            return Integer.compare(this.patch, o.patch);
        }
    }
}