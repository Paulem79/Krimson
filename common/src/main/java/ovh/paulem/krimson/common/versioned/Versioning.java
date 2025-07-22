package ovh.paulem.krimson.common.versioned;

import org.bukkit.Bukkit;

public class Versioning {
    private static final String[] mcParts = getMcParts();

    private static String[] getMcParts() {
        if(mcParts != null) return mcParts;

        String version = Bukkit.getVersion();
        String[] parts = version.substring(version.indexOf("MC: ") + 4, version.length() - 1).split("\\.");

        // 1.21 is 1.21.0
        if (parts.length < 3) {
            parts = new String[]{parts[0], parts[1], "0"};
        }

        return parts;
    }

    public static boolean isPost(int v) {
        String[] mcParts = getMcParts();
        return Integer.parseInt(mcParts[1]) > v || (Integer.parseInt(mcParts[1]) == v && Integer.parseInt(mcParts[2]) >= 1);
    }

    public static boolean isPost(int v, int r) {
        String[] mcParts = getMcParts();
        return Integer.parseInt(mcParts[1]) > v || (Integer.parseInt(mcParts[1]) == v && Integer.parseInt(mcParts[2]) > r);
    }

    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
