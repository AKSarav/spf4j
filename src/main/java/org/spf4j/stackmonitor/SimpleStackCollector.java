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
package org.spf4j.stackmonitor;

import java.util.Map;

/**
 *
 * @author zoly
 */
public class SimpleStackCollector extends AbstractStackCollector {

    @Override
    public void sample() {
        Map<Thread, StackTraceElement[]> stackDump = Thread.getAllStackTraces();
        recordStackDump(stackDump);
    }

    private void recordStackDump(Map<Thread, StackTraceElement[]> stackDump) {
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackDump.entrySet()) {
            StackTraceElement[] stackTrace = entry.getValue();
            if (stackTrace.length > 0 && !entry.getKey().equals(Thread.currentThread())) {
                addSample(stackTrace);
            }
        }
    }
}
