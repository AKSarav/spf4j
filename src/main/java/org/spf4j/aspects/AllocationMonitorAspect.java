
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
package org.spf4j.aspects;

import org.spf4j.base.InstrumentationHelper;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.RecorderFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

/**
 *
 * @author zoly
 */
@Aspect
public final class AllocationMonitorAspect {

    
    private static final boolean RECORD_OBJECT_SIZE =
            Boolean.valueOf(System.getProperty("perf.allocations.recordSize", "true"));
    
   private static final MeasurementRecorderSource RECORDER;
   
   static {
       if (RECORD_OBJECT_SIZE) {
           RECORDER = RecorderFactory.createScalableCountingRecorderSource("allocations", "bytes",
            Integer.valueOf(System.getProperty("perf.allocations.sampleTimeMillis", "300000")));
       } else {
           RECORDER = RecorderFactory.createScalableCountingRecorderSource("allocations", "instances",
            Integer.valueOf(System.getProperty("perf.allocations.sampleTimeMillis", "300000")));
       }
   }
       
    @AfterReturning(pointcut = "call(*.new(..))", returning = "obj", argNames = "jp,obj")
    public void afterAllocation(final JoinPoint jp, final Object obj) {
        if (RECORD_OBJECT_SIZE) {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).
                    record(InstrumentationHelper.getObjectSize(obj));
        } else {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).record(1);
        }
    }
}
