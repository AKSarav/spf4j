package org.spf4j.base;

import com.google.common.annotations.GwtCompatible;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Special methods to use for character sequences...
 *
 * @author zoly
 */
@GwtCompatible
public final class CharSequences {

  private CharSequences() {
  }

  /**
   * compare s to t.
   *
   * @param s
   * @param t
   * @return
   * @deprecated use compare.
   */
  @Deprecated
  public static int compareTo(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return compare(s, t);
  }

  public static int compare(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return compare(s, 0, s.length(), t, 0, t.length());
  }

  public static int compare(@Nonnull final CharSequence s, final int sLength,
          @Nonnull final CharSequence t, final int tLength) {
    return compare(s, 0, sLength, t, 0, tLength);
  }


  /**
   * compare 2 CharSequence fragments.
   * @param s the charsequence to compare
   * @param sFrom the index for the first chars to compare.
   * @param sLength the number of characters to compare.
   * @param t the charsequence to compare to
   * @param tFrom the index for the first character to compare to.
   * @param tLength the number of characters to compare to.
   * @return
   */
  public static int compare(@Nonnull final CharSequence s, final int sFrom, final int sLength,
          @Nonnull final CharSequence t, final int tFrom, final int tLength) {

    int lim = Math.min(sLength, tLength);
    int i = sFrom, j = tFrom;
    int sTo = sFrom + lim;
    while (i < sTo) {
      char c1 = s.charAt(i);
      char c2 = t.charAt(j);
      if (c1 != c2) {
        return c1 - c2;
      }
      i++;
      j++;
    }
    return sLength - tLength;
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
    @SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
    public String toString() {
      if (underlyingSequence instanceof String) {
        return ((String) underlyingSequence).substring(startIdx, startIdx + length);
      } else if (underlyingSequence instanceof StringBuilder) {
        return ((StringBuilder) underlyingSequence).substring(startIdx, startIdx + length);
      } else {
        char[] chars = new char[length];
        int idx = startIdx;
        for (int i = 0; i < length; i++, idx++) {
          chars[i] = underlyingSequence.charAt(idx);
        }
        return new String(chars);
      }
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

  public static Appendable lineNumbered(final int startLineNr, final Appendable appendable)
          throws IOException {
    return lineNumbered(startLineNr, appendable, IntAppender.CommentNumberAppender.INSTANCE);
  }

  public static Appendable lineNumbered(final int startLineNr, final Appendable appendable, final IntAppender ia)
          throws IOException {
    ia.append(startLineNr, appendable);
    return new Appendable() {
      private int lineNr = startLineNr + 1;

      @Override
      public Appendable append(final CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
      }

      @Override
      public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
        int lastIdx = start;
        for (int i = start; i < end; i++) {
          if (csq.charAt(i) == '\n') {
            int next = i + 1;
            appendable.append(csq, lastIdx, next);
            ia.append(lineNr++, appendable);
            lastIdx = next;
          }
        }
        if (lastIdx < end) {
          appendable.append(csq, lastIdx, end);
        }
        return this;
      }

      @Override
      public Appendable append(final char c) throws IOException {
        appendable.append(c);
        if (c == '\n') {
          ia.append(lineNr++, appendable);
        }
        return this;
      }
    };
  }

  public static CharSequence toLineNumbered(final int startLineNr, final CharSequence source) {
    return toLineNumbered(startLineNr, source, IntAppender.CommentNumberAppender.INSTANCE);
  }

  public static CharSequence toLineNumbered(final int startLineNr, final CharSequence source, final IntAppender ia) {
    int length = source.length();
    StringBuilder destination = new StringBuilder(length + 6 * length / 80);
    try {
      lineNumbered(startLineNr, destination, ia).append(source);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return destination;
  }

  /**
   * A more flexible version of Integer.parseInt.
   *
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(final CharSequence s) {
    return parseInt(s, 10);
  }

  /**
   * A more flexible version of Integer.parseInt.
   *
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(final CharSequence cs, final int radix) {
    /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
     */

    if (cs == null) {
      throw new NumberFormatException("cs is null for radix = " + radix);
    }

    if (radix < Character.MIN_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " less than Character.MIN_RADIX");
    }

    if (radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " greater than Character.MAX_RADIX");
    }

    int result = 0;
    boolean negative = false;
    int len = cs.length();

    if (len > 0) {
      int i = 0;
      int limit = -Integer.MAX_VALUE;
      int multmin;
      int digit;
      char firstChar = cs.charAt(0);
      if (firstChar < '0') { // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true;
          limit = Integer.MIN_VALUE;
        } else if (firstChar != '+') {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }

        if (len == 1) { // Cannot have lone "+" or "-"
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        i++;
      }
      multmin = limit / radix;
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(cs.charAt(i++), radix);
        if (digit < 0) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        if (result < multmin) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result *= radix;
        if (result < limit + digit) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
    }
    return negative ? result : -result;
  }

  /**
   * A more flexible version of Long.parseLong.
   *
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(final CharSequence cs) {
    return parseLong(cs, 10);
  }

  /**
   * A more flexible version of Long.parseLong.
   *
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(final CharSequence cs, final int radix) {
    if (cs == null) {
      throw new NumberFormatException("cs is null for radix = " + radix);
    }

    if (radix < Character.MIN_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " less than Character.MIN_RADIX");
    }
    if (radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " greater than Character.MAX_RADIX");
    }

    long result = 0;
    boolean negative = false;
    int len = cs.length();

    if (len > 0) {
      int i = 0;
      long limit = -Long.MAX_VALUE;
      long multmin;
      int digit;
      char firstChar = cs.charAt(0);
      if (firstChar < '0') { // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true;
          limit = Long.MIN_VALUE;
        } else if (firstChar != '+') {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }

        if (len == 1) { // Cannot have lone "+" or "-"
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        i++;
      }
      multmin = limit / radix;
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(cs.charAt(i++), radix);
        if (digit < 0) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        if (result < multmin) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result *= radix;
        if (result < limit + digit) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
    }
    return negative ? result : -result;
  }

  public static boolean containsAnyChar(final CharSequence string, final char... chars) {
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      if (Arrays.search(chars, c) >= 0) {
        return true;
      }
    }
    return false;
  }

  public static boolean isValidFileName(@Nonnull final CharSequence fileName) {
    return !containsAnyChar(fileName, '/', '\\');
  }

  public static <T extends CharSequence> T validatedFileName(@Nonnull final T fileName) {
    if  (!isValidFileName(fileName)) {
      throw new IllegalArgumentException("Invalid file name: " + fileName);
    }
    return fileName;
  }


}
