/*
 * Copyright 2018 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.test.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.base.Throwables;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.io.ObjectAppenderSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public class LogPrinter implements LogHandler {

  private static final String PRINTED = "PRINTED";

  private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

  private final Level minLogged;

  private static final class Buffer {

    private final ByteArrayBuilder bab;

    private final Writer writer;

    private final EscapeJsonStringAppendableWrapper writerEscaper;

    Buffer() {
      bab = new ByteArrayBuilder(512, ArraySuppliers.Bytes.JAVA_NEW);
      writer = new BufferedWriter(new OutputStreamWriter(bab, Charset.defaultCharset()));
      writerEscaper = new EscapeJsonStringAppendableWrapper(writer);
    }

    public void clear() {
      bab.reset();
    }

    public ByteArrayBuilder getBab() {
      return bab;
    }

    public Writer getWriter() {
      return writer;
    }

    public EscapeJsonStringAppendableWrapper getWriterEscaper() {
      return writerEscaper;
    }

    public byte[] getBytes() throws IOException {
      writer.flush();
      return bab.getBuffer();
    }

    public int size() {
      return bab.size();
    }

  }

  private static final ThreadLocal<Buffer> TL_BUFFER = new ThreadLocal<Buffer>() {
    @Override
    protected Buffer initialValue() {
      return new Buffer();
    }

  };

  public LogPrinter(final Level minLogged) {
    this.minLogged = minLogged;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handles(final Level level) {
    return level.ordinal() >= minLogged.ordinal();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogRecord handle(final LogRecord record) {
    if (record.hasAttachment(PRINTED)) {
      return record;
    }
    try {
      Buffer buff = TL_BUFFER.get();
      buff.clear();
      print(record, buff.getWriter(), buff.getWriterEscaper());
      if (record.getLevel() == Level.ERROR) {
        System.err.write(buff.getBytes(), 0, buff.size());
        System.err.flush();
      } else {
        System.out.write(buff.getBytes(), 0, buff.size());
        System.out.flush();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    record.attach(PRINTED);
    return record;
  }

  private void print(final LogRecord record, final Appendable wr, final EscapeJsonStringAppendableWrapper wrapper)
          throws IOException {
    FMT.formatTo(Instant.ofEpochMilli(record.getTimeStamp()), wr);
    wr.append(' ');
    String level = record.getLevel().toString();
    wr.append(level);
    int ll = level.length();
    if (ll < 6) {
      for (int i = 0, l = 6 - ll; i < l; i++) {
        wr.append(' ');
      }
    }
    wr.append('"');
    wrapper.append(record.getThread().getName());
    wr.append("\" \"");
    Object[] arguments = record.getArguments();
    int i = Slf4jMessageFormatter.format(this::exHandle, 0, wrapper, record.getFormat(),
            ObjectAppenderSupplier.TO_STRINGER, arguments);
    wr.append("\" ");
    Throwable t = null;
    if (i < arguments.length) {
      boolean first = true;
      for (; i < arguments.length; i++) {
        Object arg = arguments[i];
        if (arg instanceof Throwable) {
          if (t == null) {
            t = (Throwable) arg;
          } else {
            t.addSuppressed(t); // not ideal
          }
        } else {
          if (!first) {
            wr.append(", ");
          } else {
            wr.append('[');
            first = false;
          }
          printObject(arg, wr, wrapper);
        }
      }
      if (!first) {
        wr.append(']');
      }
    }
    wr.append(' ');
    Throwables.writeAbreviatedClassName(record.getLogger().getName(), wr);
    if (t != null) {
      wr.append('\n');
      Throwables.writeTo(t, wr, Throwables.PackageDetail.SHORT);
    }
    wr.append('\n');
  }

  private static void printObject(@Nullable final Object obj,
          final Appendable wr, final EscapeJsonStringAppendableWrapper wrapper) throws IOException {
    if (obj == null) {
      wr.append("null");
    } else {
      wr.append('"');
      wrapper.append(obj.toString());
      wr.append('"');
    }
  }

  private void exHandle(final Object obj, final Appendable sbuf, final Throwable t) throws IOException {
    String className = obj.getClass().getName();
    sbuf.append("[FAILED toString() for ");
    sbuf.append(className);
    sbuf.append("]{");
    Throwables.writeTo(t, sbuf, Throwables.PackageDetail.SHORT);
    sbuf.append('}');
  }

}
