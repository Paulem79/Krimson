package net.paulem.krimson.utils;

import java.util.Optional;

public class ClassUtils {
    /**
     * Attempts to retrieve the {@link Class} object for the fully qualified class name provided.
     *
     * @param className the fully qualified name of the class to retrieve
     * @return an {@link Optional} containing the {@link Class} object if found, or an empty {@link Optional} if the class is not found
     */
    public static Optional<Class<?>> getClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if the given class is present in the classpath.
     *
     * @param className the fully qualified name of the class to check
     * @return true if the class is present, false otherwise
     */
    public static boolean isClassPresent(String className) {
        return getClass(className).isPresent();
    }

    // TODO: Use EntityRemoveEvent for 1.20.4 and above
    public static boolean hasEntityRemoveEvent() {
        return isClassPresent("org.bukkit.event.entity.EntityRemoveEvent");
    }
}
