package java.text;

import java.util.Comparator;
import java.util.Locale;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.Intl;

/** compares strings by the rules of a locale, which the page's Intl.Collator knows */
public abstract class Collator implements Comparator<Object>, Cloneable {

    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int TERTIARY = 2;
    public static final int IDENTICAL = 3;

    public static final int NO_DECOMPOSITION = 0;
    public static final int CANONICAL_DECOMPOSITION = 1;
    public static final int FULL_DECOMPOSITION = 2;

    private int strength = TERTIARY;
    private int decomposition = CANONICAL_DECOMPOSITION;

    protected Collator() {
    }

    public static Collator getInstance() {
        return getInstance(Locale.getDefault());
    }

    public static Collator getInstance(Locale desiredLocale) {
        return new IntlCollator(CLDRHelper.tag(desiredLocale.getLanguage(),
                desiredLocale.getCountry()));
    }

    public static Locale[] getAvailableLocales() {
        return Locale.getAvailableLocales();
    }

    public abstract int compare(String source, String target);

    @Override
    public int compare(Object o1, Object o2) {
        return compare((String) o1, (String) o2);
    }

    public abstract CollationKey getCollationKey(String source);

    public boolean equals(String source, String target) {
        return compare(source, target) == 0;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int newStrength) {
        if (newStrength < PRIMARY || newStrength > IDENTICAL) {
            throw new IllegalArgumentException("Incorrect comparison level.");
        }
        strength = newStrength;
    }

    public int getDecomposition() {
        return decomposition;
    }

    public void setDecomposition(int decompositionMode) {
        if (decompositionMode < NO_DECOMPOSITION || decompositionMode > FULL_DECOMPOSITION) {
            throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        decomposition = decompositionMode;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Collator other && getClass() == other.getClass()
                && strength == other.strength && decomposition == other.decomposition;
    }

    @Override
    public int hashCode() {
        return strength * 31 + decomposition;
    }

    private static final class IntlCollator extends Collator {
        private final String tag;

        IntlCollator(String tag) {
            this.tag = tag;
        }

        @Override
        public int compare(String source, String target) {
            String sensitivity = switch (getStrength()) {
                case PRIMARY -> "base";
                case SECONDARY -> "accent";
                default -> "variant";
            };
            int result = Integer.signum(Intl.compare(tag, sensitivity, source, target));
            if (result == 0 && getStrength() == IDENTICAL) {
                result = Integer.signum(source.compareTo(target));
            }
            return result;
        }

        @Override
        public CollationKey getCollationKey(String source) {
            return source == null ? null : new Key(source, this);
        }

        @Override
        public boolean equals(Object that) {
            return super.equals(that) && tag.equals(((IntlCollator) that).tag);
        }

        @Override
        public int hashCode() {
            return tag.hashCode() * 31 + getStrength();
        }
    }

    private static final class Key extends CollationKey {
        private final Collator collator;

        Key(String source, Collator collator) {
            super(source);
            this.collator = collator;
        }

        @Override
        public int compareTo(CollationKey target) {
            return collator.compare(getSourceString(), target.getSourceString());
        }

        @Override
        public byte[] toByteArray() {
            String source = getSourceString();
            byte[] bytes = new byte[source.length() * 2];
            for (int i = 0; i < source.length(); i++) {
                bytes[i * 2] = (byte) (source.charAt(i) >> 8);
                bytes[i * 2 + 1] = (byte) source.charAt(i);
            }
            return bytes;
        }
    }
}
