package dev.flagon.platform.fs;

/**
 * absolute, {@code /}-separated paths, resolved without touching a store
 */
public final class FilePaths {

    public static final String ROOT = "/";

    private FilePaths() {
    }

    /**
     @return an absolute path with no empty, {@code .} or {@code ..} segments and no trailing separator
     */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return ROOT;
        }
        String[] segments = path.split("/");
        String[] kept = new String[segments.length];
        int depth = 0;
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                // the root has no parent, so climbing past it stays there, as it does on posix
                if (depth > 0) {
                    depth--;
                }
                continue;
            }
            kept[depth++] = segment;
        }
        if (depth == 0) {
            return ROOT;
        }
        StringBuilder resolved = new StringBuilder();
        for (int at = 0; at < depth; at++) {
            resolved.append('/').append(kept[at]);
        }
        return resolved.toString();
    }

    /** @return the path with {@code name} appended, normalised */
    public static String join(String path, String name) {
        return normalize(path + "/" + name);
    }

    /** @return the containing directory; the root is its own parent */
    public static String parent(String path) {
        String normalized = normalize(path);
        int separator = normalized.lastIndexOf('/');
        return separator <= 0 ? ROOT : normalized.substring(0, separator);
    }

    /** @return the last segment, or an empty string for the root */
    public static String name(String path) {
        String normalized = normalize(path);
        return normalized.equals(ROOT) ? "" : normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    public static boolean isRoot(String path) {
        return normalize(path).equals(ROOT);
    }
}
