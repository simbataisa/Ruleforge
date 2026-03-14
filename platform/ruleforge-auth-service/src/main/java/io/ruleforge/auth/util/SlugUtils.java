package io.ruleforge.auth.util;

/**
 * Utility for converting human-readable names to URL-safe slug strings.
 * <p>
 * Examples:
 * <ul>
 *   <li>"My First Space!" → "my-first-space"</li>
 *   <li>"Q4 Sales & Marketing" → "q4-sales-marketing"</li>
 *   <li>"  hello  world  " → "hello-world"</li>
 * </ul>
 */
public final class SlugUtils {

    private SlugUtils() {}

    public static String toSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9]+", "-")
                   .replaceAll("^-|-$", "");
    }
}
