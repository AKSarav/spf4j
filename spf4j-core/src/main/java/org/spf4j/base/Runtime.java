
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
package org.spf4j.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class Runtime {

    private Runtime() {
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
    public static void goDownWithError(final Throwable t, final int exitCode) {
        try {
            LOGGER.error("Unrecoverable Error, going down", t);
        } finally {
            try {
                t.printStackTrace();
            } finally {
                System.exit(exitCode);
            }
        }
    }
    public static final String TMP_FOLDER = System.getProperty("java.io.tmpdir");
    public static final int PID;
    public static final String OS_NAME;
    public static final String PROCESS_NAME;

    static {
        PROCESS_NAME = ManagementFactory.getRuntimeMXBean().getName();
        int atIdx = PROCESS_NAME.indexOf('@');
        if (atIdx < 0) {
            PID = -1;
        } else {
            PID = Integer.parseInt(PROCESS_NAME.substring(0, atIdx));
        }
        OS_NAME = System.getProperty("os.name");
    }
    public static final String MAC_OS_X_OS_NAME = "Mac OS X";
    private static final File FD_FOLDER = new File("/proc/" + PID + "/fd");

    public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException {
        if (OS_NAME.equals(MAC_OS_X_OS_NAME)) {
            LineCountCharHandler handler = new LineCountCharHandler();
            run("/usr/sbin/lsof -p " + PID, handler);
            return handler.getLineCount() - 1;
        } else {
            if (FD_FOLDER.exists()) {
                return FD_FOLDER.list().length;
            } else {
                return -1;
            }
        }
    }

    @Nullable
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public static String getLsofOutput() throws IOException, InterruptedException, ExecutionException {
        File lsofFile = new File("/usr/sbin/lsof");
        if (!lsofFile.exists()) {
            lsofFile = new File("/usr/bin/lsof");
            if (!lsofFile.exists()) {
                lsofFile = new File("/usr/local/bin/lsof");
                if (!lsofFile.exists()) {
                    return null;
                }
            }
        }
        StringBuilderCharHandler handler = new StringBuilderCharHandler();
        run(lsofFile.getAbsolutePath() + " -p " + PID, handler);
        return handler.toString();
    }

    public interface ProcOutputHandler {

        void handleStdOut(int character);

        void handleStdErr(int character);
    }

    public static int run(final String command, final ProcOutputHandler handler)
            throws IOException, InterruptedException, ExecutionException {
        Process proc = java.lang.Runtime.getRuntime().exec(command);
        InputStream pos = proc.getInputStream();
        try {
            final InputStream pes = proc.getErrorStream();
            try {
                OutputStream pis = proc.getOutputStream();
                try {
                    Future<?> esh = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {
                                           @Override
                                           public void doRun() throws Exception {
                                               int eos;
                                               while ((eos = pes.read()) >= 0) {
                                                   handler.handleStdErr(eos);
                                               }
                                           }
                                       });
                    int cos;
                    while ((cos = pos.read()) >= 0) {
                        handler.handleStdOut(cos);
                    }
                    esh.get();
                    
                } finally {
                    pis.close();
                }
            } finally {
                pos.close();
            }
        } finally {
            pos.close();
        }
        return proc.exitValue();
    }

    private static class LineCountCharHandler implements ProcOutputHandler {

        public LineCountCharHandler() {
            lineCount = 0;
        }
        private int lineCount;

        @Override
        public void handleStdOut(final int c) {
            if (c == '\n') {
                lineCount++;
            }
        }

        public int getLineCount() {
            return lineCount;
        }

        @Override
        public void handleStdErr(final int character) {
        }
    }

    private static class StringBuilderCharHandler implements ProcOutputHandler {

        public StringBuilderCharHandler() {
            builder = new StringBuilder();
        }
        private StringBuilder builder;

        @Override
        public void handleStdOut(final int c) {
            builder.append((char) c);
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        @Override
        public void handleStdErr(final int c) {
            builder.append(c);
        }
    }
    private static final LinkedList<Runnable> SHUTDOWN_HOOKS = new LinkedList<Runnable>();

    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                synchronized (SHUTDOWN_HOOKS) {
                    for (Runnable runnable : SHUTDOWN_HOOKS) {
                        try {
                            runnable.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "tsdb shutdown"));
    }

    public static void addHookAtBeginning(final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            SHUTDOWN_HOOKS.addFirst(runnable);
        }
    }

    public static void addHookAtEnd(final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            SHUTDOWN_HOOKS.addLast(runnable);
        }
    }
}
