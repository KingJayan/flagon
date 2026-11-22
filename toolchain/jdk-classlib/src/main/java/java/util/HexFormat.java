package java.util;

/** bytes and numbers as hexadecimal text, with an optional delimiter, prefix and suffix */
public final class HexFormat {

    private static final char[] LOWER = "0123456789abcdef".toCharArray();
    private static final char[] UPPER = "0123456789ABCDEF".toCharArray();
    private static final HexFormat PLAIN = new HexFormat("", "", "", LOWER);

    private final String delimiter;
    private final String prefix;
    private final String suffix;
    private final char[] digits;

    private HexFormat(String delimiter, String prefix, String suffix, char[] digits) {
        this.delimiter = Objects.requireNonNull(delimiter, "delimiter");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.suffix = Objects.requireNonNull(suffix, "suffix");
        this.digits = digits;
    }

    public static HexFormat of() {
        return PLAIN;
    }

    public static HexFormat ofDelimiter(String delimiter) {
        return new HexFormat(delimiter, "", "", LOWER);
    }

    public HexFormat withDelimiter(String delimiter) {
        return new HexFormat(delimiter, prefix, suffix, digits);
    }

    public HexFormat withPrefix(String prefix) {
        return new HexFormat(delimiter, prefix, suffix, digits);
    }

    public HexFormat withSuffix(String suffix) {
        return new HexFormat(delimiter, prefix, suffix, digits);
    }

    public HexFormat withUpperCase() {
        return new HexFormat(delimiter, prefix, suffix, UPPER);
    }

    public HexFormat withLowerCase() {
        return new HexFormat(delimiter, prefix, suffix, LOWER);
    }

    public String delimiter() {
        return delimiter;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isUpperCase() {
        return digits == UPPER;
    }

    public String formatHex(byte[] bytes) {
        return formatHex(bytes, 0, bytes.length);
    }

    public String formatHex(byte[] bytes, int fromIndex, int toIndex) {
        return formatHex(new StringBuilder(), bytes, fromIndex, toIndex).toString();
    }

    public <A extends Appendable> A formatHex(A out, byte[] bytes) {
        return formatHex(out, bytes, 0, bytes.length);
    }

    public <A extends Appendable> A formatHex(A out, byte[] bytes, int fromIndex, int toIndex) {
        Objects.requireNonNull(out, "out");
        Objects.checkFromToIndex(fromIndex, toIndex, bytes.length);
        try {
            for (int at = fromIndex; at < toIndex; at++) {
                if (at > fromIndex) {
                    out.append(delimiter);
                }
                out.append(prefix);
                out.append(digits[(bytes[at] >> 4) & 0xF]);
                out.append(digits[bytes[at] & 0xF]);
                out.append(suffix);
            }
        } catch (java.io.IOException failed) {
            throw new java.io.UncheckedIOException(failed.getMessage(), failed);
        }
        return out;
    }

    public byte[] parseHex(CharSequence string) {
        return parseHex(string, 0, string.length());
    }

    public byte[] parseHex(CharSequence string, int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, string.length());
        String text = string.subSequence(fromIndex, toIndex).toString();
        if (text.isEmpty()) {
            return new byte[0];
        }
        int each = prefix.length() + 2 + suffix.length();
        int stride = each + delimiter.length();
        if ((text.length() + delimiter.length()) % stride != 0) {
            throw new IllegalArgumentException("extra or missing delimiters or values consisting"
                    + " of prefix, two hexadecimal digits, and suffix");
        }
        byte[] out = new byte[(text.length() + delimiter.length()) / stride];
        for (int index = 0; index < out.length; index++) {
            int at = index * stride;
            if (index > 0 && !text.startsWith(delimiter, at - delimiter.length())) {
                throw new IllegalArgumentException("found: \"" + text.substring(
                        at - delimiter.length(), at) + "\", expected: \"" + delimiter + "\"");
            }
            if (!text.startsWith(prefix, at)
                    || !text.startsWith(suffix, at + prefix.length() + 2)) {
                throw new IllegalArgumentException("found: \"" + text.substring(at, at + each)
                        + "\", expected a prefix of \"" + prefix + "\" and a suffix of \""
                        + suffix + "\"");
            }
            int digitsAt = at + prefix.length();
            out[index] = (byte) (fromHexDigit(text.charAt(digitsAt)) << 4
                    | fromHexDigit(text.charAt(digitsAt + 1)));
        }
        return out;
    }

    public char toLowHexDigit(int value) {
        return digits[value & 0xF];
    }

    public char toHighHexDigit(int value) {
        return digits[(value >> 4) & 0xF];
    }

    public <A extends Appendable> A toHexDigits(A out, byte value) {
        try {
            out.append(toHighHexDigit(value)).append(toLowHexDigit(value));
        } catch (java.io.IOException failed) {
            throw new java.io.UncheckedIOException(failed.getMessage(), failed);
        }
        return out;
    }

    public String toHexDigits(byte value) {
        return digits(value, 2);
    }

    public String toHexDigits(char value) {
        return digits(value, 4);
    }

    public String toHexDigits(short value) {
        return digits(value, 4);
    }

    public String toHexDigits(int value) {
        return digits(value, 8);
    }

    public String toHexDigits(long value) {
        return digits(value, 16);
    }

    public String toHexDigits(long value, int digitCount) {
        if (digitCount < 0 || digitCount > 16) {
            throw new IllegalArgumentException("number of digits: " + digitCount);
        }
        return digits(value, digitCount);
    }

    private String digits(long value, int count) {
        char[] out = new char[count];
        for (int at = count - 1; at >= 0; at--) {
            out[at] = digits[(int) (value & 0xF)];
            value >>>= 4;
        }
        return new String(out);
    }

    public static boolean isHexDigit(int ch) {
        return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

    public static int fromHexDigit(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new NumberFormatException("not a hexadecimal digit: \"" + (char) ch + "\" = " + ch);
    }

    public static int fromHexDigits(CharSequence string) {
        return fromHexDigits(string, 0, string.length());
    }

    public static int fromHexDigits(CharSequence string, int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, string.length());
        if (toIndex - fromIndex > 8) {
            throw new IllegalArgumentException("string length greater than 8: "
                    + (toIndex - fromIndex));
        }
        return (int) fromHexDigitsToLong(string, fromIndex, toIndex);
    }

    public static long fromHexDigitsToLong(CharSequence string) {
        return fromHexDigitsToLong(string, 0, string.length());
    }

    public static long fromHexDigitsToLong(CharSequence string, int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, string.length());
        if (toIndex - fromIndex > 16) {
            throw new IllegalArgumentException("string length greater than 16: "
                    + (toIndex - fromIndex));
        }
        long value = 0;
        for (int at = fromIndex; at < toIndex; at++) {
            value = value << 4 | fromHexDigit(string.charAt(at));
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HexFormat that && delimiter.equals(that.delimiter)
                && prefix.equals(that.prefix) && suffix.equals(that.suffix)
                && digits == that.digits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delimiter, prefix, suffix, isUpperCase());
    }

    @Override
    public String toString() {
        return "uppercase: " + isUpperCase() + ", delimiter: \"" + delimiter + "\", prefix: \""
                + prefix + "\", suffix: \"" + suffix + "\"";
    }
}
