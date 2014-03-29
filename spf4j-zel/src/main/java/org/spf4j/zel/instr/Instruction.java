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
package org.spf4j.zel.instr;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

/**
 * <p>
 * Title: VM Instruction</p>
 * <p>
 * Description: Abstract Instruction Implementation</p>
 *
 * @author zoly
 */
public abstract class Instruction implements Serializable {

    /**
     * Instruction execution
     *
     * @param context ExecutionContext
     * @throws ZExecutionException
     * @throws java.lang.InterruptedException
     * @returns relative instruction pointer for next instruction.
     */
    public abstract int execute(ExecutionContext context)
            throws ZExecutionException, InterruptedException, SuspendedException;

    public abstract Object [] getParameters();
    
    
    /**
     * Outputs Instruction Name - use for debug purposes ...
     *
     * @return String
     */
    @Override
    public final String toString() {
        if (getParameters().length > 0) {
           return this.getClass().getSimpleName() + "(" + Arrays.toString(getParameters()) + ")";
        } else {
            return this.getClass().getSimpleName();
        }
    }


    @Nullable
    public static Instruction getInstruction(final String packageName, final String instrName) {
        Class<Instruction> clasz;
        try {
            clasz = (Class<Instruction>) Class.forName(packageName + "." + instrName);
        } catch (ClassNotFoundException ex) {
            return null;
        }
        Field[] fields = clasz.getFields();
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase("instance")) {
                try {
                    return (Instruction) field.get(null);
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return null;
    }

}
