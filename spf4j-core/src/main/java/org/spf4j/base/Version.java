package org.spf4j.base;

import com.google.common.primitives.Ints;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Version implements Comparable<Version> {

    private final Comparable[] components;

    private final String image;

    public Version(final CharSequence version) {
        this.image = version.toString();
        List<Comparable<?>> comps = new ArrayList<>(4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            final int length = sb.length();
            if (c == '.') {
                addPart(sb, comps);
                sb.setLength(0);
            } else if (Character.isDigit(c)) {
                if (length > 0) {
                    char prev = sb.charAt(length - 1);
                    if (!Character.isDigit(prev)) {
                        comps.add(sb.toString());
                        sb.setLength(0);
                    }
                }
                sb.append(c);
            } else {
                if (length > 0) {
                    char prev = sb.charAt(length - 1);
                    if (Character.isDigit(prev)) {
                        comps.add(Integer.valueOf(sb.toString()));
                        sb.setLength(0);
                    }
                }
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
           addPart(sb, comps);
        }
        components = comps.toArray(new Comparable[comps.size()]);
    }

    private static void addPart(final StringBuilder sb, final List<Comparable<?>> comps) {
        final String strPart = sb.toString();
        Integer nr = Ints.tryParse(strPart);
        if (nr == null) {
            comps.add(strPart);
        } else {
            comps.add(nr);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(final Version o) {
        return Comparables.compareArrays(components, o.components);
    }

    @Override
    public int hashCode() {
        return image.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return this.compareTo((Version) obj) == 0;
    }

    public String getImage() {
        return image;
    }

    public Comparable[] getComponents() {
        return components.clone();
    }

    public Comparable getComponent(final int pos) {
      return components[pos];
    }

    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public int getMajor() {
      return (int) components[0];
    }

    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public int getMinor() {
      return (int) components[1];
    }

    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public int getPatch() {
      return (int) components[2];
    }

    public int getNrComponents() {
      return components.length;
    }

    @Override
    public String toString() {
        return "Version{" + "components=" + Arrays.toString(components) + '}';
    }

}
