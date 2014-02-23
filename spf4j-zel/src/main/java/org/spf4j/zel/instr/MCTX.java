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

import java.math.MathContext;
import org.spf4j.zel.operators.Operator;
import org.spf4j.zel.vm.ExecutionContext;


/**
 * Add takes two objects from the top of the stack and puts the sum back
 *
 * @author zoly
 * @version 1.0
 */
public final class MCTX extends Instruction {

    private static final long serialVersionUID = 6127414006563169983L;

    private MCTX() {
    }
    
    /**
     * ADD Instruction microcode
     * if any of the operands are null the result is null
     * @param context ExecutionContext
     */
    @Override
    public void execute(final ExecutionContext context) {
        int precission = (Integer) context.code.get(context.ip + 1);
        switch(precission) {
            case 32:
                context.mathContext = MathContext.DECIMAL32;
                break;
            case 64:
                context.mathContext = MathContext.DECIMAL64;
                break;
            case 128:
                context.mathContext = MathContext.DECIMAL128;
                break;
            default:
                context.mathContext = MathContext.DECIMAL128;
        }
        Operator.MATH_CONTEXT.set(context.mathContext);
        context.ip += 2;
    }
    /**
     * Add instance
     */
    public static final Instruction INSTANCE = new MCTX();


}
