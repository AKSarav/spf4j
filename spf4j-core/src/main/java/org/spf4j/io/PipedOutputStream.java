/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.spf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Equivalent to Java piped input/output stream.
 *
 * This implementation supports timeouts, timeout are specified by setting the org.spf4j.base.runtime.DEADLINE thread
 * local.
 *
 * Implementation supports multiple readers and writers.
 *
 * This implementation should be slightly faster than the JDK implementation.
 *
 * @author zoly
 */
@ThreadSafe
//CHECKSTYLE IGNORE InnerAssignment FOR NEXT 2000 LINES
public final class PipedOutputStream extends OutputStream {

    private final byte[] buffer;

    private final Object sync = new Object();

    private int startIdx;
    private int endIdx;
    private boolean writerClosed;
    private int nrReadStreams;

    public PipedOutputStream() {
        this(8192);
    }

    public PipedOutputStream(final int bufferSize) {
        buffer = ArraySuppliers.Bytes.SUPPLIER.get(bufferSize);
        startIdx = 0;
        endIdx = 0;
        writerClosed = false;
        nrReadStreams = 0;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        long deadline = org.spf4j.base.Runtime.DEADLINE.get();
        int bytesWritten = 0;
        while (bytesWritten < len) {
            synchronized (sync) {
                int availableToWrite = 0;
                while (!writerClosed &&  nrReadStreams > 0 && (availableToWrite = availableToWrite()) < 1) {
                    long timeToWait = deadline - System.currentTimeMillis();
                    if (timeToWait <= 0) {
                        throw new IOException("Write timed out, deadline was: " + deadline);
                    }
                    try {
                        sync.wait(timeToWait);
                    } catch (InterruptedException ex) {
                        throw new IOException("Interrupted while writing " + Arrays.toString(b), ex);
                    }
                }
                if (writerClosed) {
                    throw new IOException("Cannot write, stream closed " + this);
                } else if (nrReadStreams <= 0) {
                    throw new IOException("Broken pipe " + this);
                }
                availableToWrite = Math.min(availableToWrite, len - bytesWritten);
                int wrToEnd = Math.min(availableToWrite, buffer.length - endIdx);
                System.arraycopy(b, off + bytesWritten, buffer, endIdx, wrToEnd);
                endIdx += wrToEnd;
                bytesWritten += wrToEnd;
                int wrapArrond = availableToWrite - wrToEnd;
                if (wrapArrond > 0) {
                    System.arraycopy(b, off + bytesWritten, buffer, 0, wrapArrond);
                    endIdx = wrapArrond;
                    bytesWritten += wrapArrond;
                } else if (endIdx >= buffer.length) {
                    endIdx = 0;
                }
                sync.notifyAll();
            }
        }
    }

    @Override
    public void write(final int b) throws IOException {
        long deadline = org.spf4j.base.Runtime.DEADLINE.get();
        synchronized (sync) {
            while (!writerClosed && nrReadStreams > 0 && availableToWrite() < 1) {
                try {
                    long timeToWait = deadline - System.currentTimeMillis();
                    if (timeToWait <= 0) {
                        throw new IOException("Write timed out, deadline was: " + deadline);
                    }
                    sync.wait(timeToWait);
                } catch (InterruptedException ex) {
                    throw new IOException("Interrupted while writing " + b, ex);
                }
            }
            if (writerClosed) {
                throw new IOException("Cannot write stream closed " + this);
            } else if (nrReadStreams <= 0) {
                throw new IOException("Broken pipe " + this);
            }
            buffer[endIdx++] = (byte) b;
            if (endIdx >= buffer.length) {
                endIdx = 0;
            }
            sync.notifyAll();
        }
    }

    private int availableToWrite() {
        if (startIdx <= endIdx) {
            return startIdx + buffer.length - endIdx - 1;
        } else {
            return startIdx - endIdx - 1;
        }
    }

    private int availableToRead() {
        if (startIdx <= endIdx) {
            return endIdx - startIdx;
        } else {
            return buffer.length - startIdx + endIdx;
        }
    }

    @Override
    public void close() {
        try {
            synchronized (sync) {
                writerClosed = true;
                sync.notifyAll();
            }
        } finally {
            ArraySuppliers.Bytes.SUPPLIER.recycle(buffer);
        }
    }

    public InputStream getInputStream() {
        synchronized (sync) {
            nrReadStreams++;
            return new InputStream() {

                private boolean readerClosed = false;

                @Override
                public int read() throws IOException {
                    long deadline = org.spf4j.base.Runtime.DEADLINE.get();
                    synchronized (sync) {
                        int availableToRead = 0;
                        while (!readerClosed && (availableToRead = availableToRead()) < 1 && !writerClosed) {
                            long timeToWait = deadline - System.currentTimeMillis();
                            if (timeToWait <= 0) {
                                throw new IOException("Write timed out, deadline was: " + deadline);
                            }
                            try {
                                sync.wait(timeToWait);
                            } catch (InterruptedException ex) {
                                throw new IOException("Interrupted while reading from "
                                        + PipedOutputStream.this, ex);
                            }
                        }
                        if (readerClosed) {
                            throw new IOException("Reader is closed for " + PipedOutputStream.this);
                        }
                        if (availableToRead == 0) {
                            if (!writerClosed) {
                                throw new IllegalStateException("Stream must be closed");
                            }
                            return -1;
                        }
                        int result = buffer[startIdx];
                        startIdx++;
                        if (startIdx >= buffer.length) {
                            startIdx = 0;
                        }
                        sync.notifyAll();
                        return result;
                    }
                }

                @Override
                public int read(final byte[] b, final int off, final int len) throws IOException {
                    long deadline = org.spf4j.base.Runtime.DEADLINE.get();
                    int bytesWritten = 0;
                    synchronized (sync) {
                        int availableToRead = 0;
                        while (!readerClosed && (availableToRead = availableToRead()) < 1 && !writerClosed) {
                            long timeToWait = deadline - System.currentTimeMillis();
                            if (timeToWait <= 0) {
                                throw new IOException("Write timed out, deadline was: " + deadline);
                            }
                            try {
                                sync.wait(timeToWait);
                            } catch (InterruptedException ex) {
                                throw new IOException("Interrupted while reading from " + PipedOutputStream.this, ex);
                            }
                        }
                        if (readerClosed) {
                            throw new IOException("Reader is closed for " + PipedOutputStream.this);
                        }
                        if (availableToRead == 0) {
                            if (!writerClosed) {
                                throw new IllegalStateException("Stream should  be closed, " + PipedOutputStream.this);
                            }
                            return -1;
                        }
                        availableToRead = Math.min(availableToRead, len);
                        int readToEnd = Math.min(availableToRead, buffer.length - startIdx);
                        System.arraycopy(buffer, startIdx, b, off, readToEnd);
                        bytesWritten += readToEnd;
                        startIdx += readToEnd;
                        int remaining = availableToRead - readToEnd;
                        if (remaining > 0) {
                            System.arraycopy(buffer, 0, b, off + readToEnd, remaining);
                            bytesWritten += remaining;
                            startIdx = remaining;
                        } else if (startIdx >= buffer.length) {
                            startIdx = 0;
                        }

                        sync.notifyAll();
                        return bytesWritten;
                    }
                }

                @Override
                public int available() throws IOException {
                    synchronized (sync) {
                        return availableToRead();
                    }
                }

                @Override
                public void close() {
                    synchronized (sync) {
                        nrReadStreams--;
                        readerClosed = true;
                        sync.notifyAll();
                    }
                }
            };
        }

    }

    @Override
    public String toString() {
        synchronized (sync) {
            return "PipedOutputStream{" + "bufferLength=" + buffer.length + ", startIdx=" + startIdx
                    + ", endIdx=" + endIdx + ", closed=" + writerClosed + '}';
        }
    }

}
