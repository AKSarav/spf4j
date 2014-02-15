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

import org.spf4j.zel.operators.Operator;
import org.spf4j.zel.operators.Operators;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.SuspendedException;


/**
 * Add takes two objects from the top of the stack and puts the sum back
 *
 * @author zoly
 * @version 1.0
 */
public final class INC extends Instruction {

    private static final long serialVersionUID = 6127414006563169983L;

    private INC() {
    }
    
    /**
     * ADD Instruction microcode
     * if any of the operands are null the result is null
     * @param context ExecutionContext
     */
    @Override
    public void execute(final ExecutionContext context)
            throws SuspendedException {
        Object val = context.popSyncStackVal();
        if (val instanceof AssignableValue) {
            AssignableValue aval = (AssignableValue) val;
            Number nr = (Number) aval.get();
            Number result = (Number) Operators.apply(Operator.Enum.Add, nr, 1);
            aval.assign(result);
        } else {
            context.push(Operators.apply(Operator.Enum.Add, val, 1)); 
        }
        context.ip++;
    }
    /**
     * Add instance
     */
    public static final Instruction INSTANCE = new INC();


}
