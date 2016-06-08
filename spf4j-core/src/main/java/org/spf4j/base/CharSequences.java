package org.spf4j.base;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Special methods to use for character sequences...
 * @author zoly
 */
public final class CharSequences {

  private CharSequences() {
  }

  public static int compareTo(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    int i = 0;
    final int sl = s.length();
    final int tl = t.length();
    while (i < sl && i < tl) {
      char a = s.charAt(i);
      char b = t.charAt(i);
      int diff = a - b;
      if (diff != 0) {
        return diff;
      }
      i++;
    }
    return sl - tl;
  }

  public static boolean equalsNullables(@Nullable final CharSequence s, @Nullable final CharSequence t) {
    if (s == null) {
      return null == t;
    } else if (t == null) {
      return true;
    } else {
      return equals(s, t);
    }
  }

  public static boolean equals(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    final int sl = s.length();
    final int tl = t.length();
    if (sl != tl) {
      return false;
    } else {
      for (int i = 0; i < sl; i++) {
        if (s.charAt(i) != t.charAt(i)) {
          return false;
        }
      }
      return true;
    }
  }

  public static int hashcode(@Nonnull final CharSequence cs) {
    if (cs instanceof String) {
      return ((String) cs).hashCode();
    }
    int h = 0;
    int len = cs.length();
    if (len > 0) {
      int off = 0;
      for (int i = 0; i < len; i++) {
        h = 31 * h + cs.charAt(off++);
      }
    }
    return h;
  }

  public static CharSequence subSequence(@Nonnull final CharSequence seq, final int startIdx, final int endIdx) {
    if (startIdx == 0 && endIdx == seq.length()) {
      return seq;
    } else if (startIdx >= endIdx) {
      return "";
    } else {
      return new SubSequence(seq, endIdx - startIdx, startIdx);
    }
  }

  private static final class SubSequence implements CharSequence {

    private final CharSequence underlyingSequence;
    private final int length;
    private final int startIdx;

    SubSequence(final CharSequence underlyingSequence, final int length, final int startIdx) {
      this.underlyingSequence = underlyingSequence;
      this.length = length;
      this.startIdx = startIdx;
    }

    @Override
    public int length() {
      return length;
    }

    @Override
    public char charAt(final int index) {
      return underlyingSequence.charAt(startIdx + index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
      return CharSequences.subSequence(underlyingSequence, startIdx + start, startIdx + end);
    }

    @Override
    public String toString() {
      char[] chars = new char[length];
      int idx = startIdx;
      for (int i = 0; i < length; i++, idx++) {
        chars[i] = underlyingSequence.charAt(idx);
      }
      return Strings.wrap(chars);
    }

  }

  public static boolean endsWith(final CharSequence qc, final CharSequence with) {
    int l = qc.length();
    int start = l - with.length();
    if (start >= 0) {
      for (int i = start, j = 0; i < l; i++, j++) {
        if (qc.charAt(i) != with.charAt(j)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

}
