package java.net;

public final class IDN {

    public static final int ALLOW_UNASSIGNED = 0x01;
    public static final int USE_STD3_ASCII_RULES = 0x02;

    private static final String MARKER = "xn--";
    private static final int BASE = 36;
    private static final int TMIN = 1;
    private static final int TMAX = 26;
    private static final int SKEW = 38;
    private static final int DAMP = 700;
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;

    private IDN() {
    }

    public static String toASCII(String name) {
        return toASCII(name, 0);
    }

    public static String toASCII(String name, int flag) {
        StringBuilder written = new StringBuilder();
        int from = 0;
        while (from <= name.length()) {
            int dot = name.indexOf('.', from);
            int to = dot < 0 ? name.length() : dot;
            written.append(label(name.substring(from, to)));
            if (dot < 0) {
                break;
            }
            written.append('.');
            from = dot + 1;
        }
        return written.toString();
    }

    public static String toUnicode(String name) {
        return toUnicode(name, 0);
    }

    public static String toUnicode(String name, int flag) {
        return name;
    }

    private static String label(String part) {
        if (isAscii(part)) {
            return part;
        }
        return MARKER + encode(part);
    }

    private static boolean isAscii(String part) {
        for (int at = 0; at < part.length(); at++) {
            if (part.charAt(at) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private static String encode(String part) {
        StringBuilder out = new StringBuilder();
        int[] points = part.codePoints().toArray();
        int handled = 0;
        for (int point : points) {
            if (point < INITIAL_N) {
                out.append((char) point);
                handled++;
            }
        }
        int basic = handled;
        if (basic > 0) {
            out.append('-');
        }
        int n = INITIAL_N;
        int delta = 0;
        int bias = INITIAL_BIAS;
        while (handled < points.length) {
            int next = Integer.MAX_VALUE;
            for (int point : points) {
                if (point >= n && point < next) {
                    next = point;
                }
            }
            delta += (next - n) * (handled + 1);
            n = next;
            for (int point : points) {
                if (point < n) {
                    delta++;
                } else if (point == n) {
                    int q = delta;
                    for (int k = BASE;; k += BASE) {
                        int t = threshold(k, bias);
                        if (q < t) {
                            break;
                        }
                        out.append(digit(t + (q - t) % (BASE - t)));
                        q = (q - t) / (BASE - t);
                    }
                    out.append(digit(q));
                    bias = adapt(delta, handled + 1, handled == basic);
                    delta = 0;
                    handled++;
                }
            }
            delta++;
            n++;
        }
        return out.toString();
    }

    private static int threshold(int k, int bias) {
        if (k <= bias + TMIN) {
            return TMIN;
        }
        if (k >= bias + TMAX) {
            return TMAX;
        }
        return k - bias;
    }

    private static char digit(int value) {
        return (char) (value < 26 ? 'a' + value : '0' + value - 26);
    }

    private static int adapt(int delta, int count, boolean first) {
        int scaled = first ? delta / DAMP : delta / 2;
        scaled += scaled / count;
        int k = 0;
        while (scaled > ((BASE - TMIN) * TMAX) / 2) {
            scaled /= BASE - TMIN;
            k += BASE;
        }
        return k + (BASE - TMIN + 1) * scaled / (scaled + SKEW);
    }
}
