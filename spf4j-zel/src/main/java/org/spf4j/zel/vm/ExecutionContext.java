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
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Either;
import org.spf4j.base.Throwables;
import org.spf4j.concurrent.FutureBean;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.operators.Operator;
import static org.spf4j.zel.vm.Program.ExecutionType.SYNC;

/**
 * Virtual Machine Execution Context
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ExecutionContext {

    private static final long serialVersionUID = 1L;

    //CHECKSTYLE:OFF
    public MathContext mathContext;

    public final VMExecutor execService;

    public final ResultCache resultCache;

    public final Object[] mem;

    public final Object[] globalMem;

    /**
     * the program
     */
    public final Program code;

    /**
     * The Instruction pointer
     */
    public int ip;

    /**
     * The halt register
     */
    public boolean terminated;

    /**
     * The main stack
     */
    private final SimpleStack<Object> stack;

    /**
     * Standard Input
     */
    public transient final InputStream in;

    /**
     * Standard Output
     */
    public transient final PrintStream out;

    /**
     * Standard Error Output
     */
    public transient final PrintStream err;

    List<VMFuture<Object>> suspendedAt;
    //CHECKSTYLE:ON

    public void suspend(final VMFuture<Object> future) throws SuspendedException {
        suspendedAt = Arrays.asList(future);
        throw SuspendedException.INSTANCE;
    }

    public void suspend(final List<VMFuture<Object>> futures) throws SuspendedException {
        suspendedAt = futures;
        throw SuspendedException.INSTANCE;
    }

    private final boolean isChildContext;

    private ExecutionContext(final ExecutionContext parent, @Nullable final VMExecutor service, final Program program) {
        this.in = parent.in;
        this.out = parent.out;
        this.err = parent.err;
        this.mem = new Object[program.getLocalMemSize()];
        this.globalMem = parent.globalMem;
        this.execService = service;
        this.stack = new SimpleStack(8);
        this.code = program;
        this.resultCache = parent.resultCache;
        this.ip = 0;
        isChildContext = true;
    }

    /**
     * aditional constructor that allows you to set the standard Input/Output streams
     *
     * @param program
     * @param in
     * @param out
     * @param err
     */
     ExecutionContext(final Program program, final Object[] globalMem,
            @Nullable final InputStream in, @Nullable final PrintStream out, @Nullable final PrintStream err,
            @Nullable final VMExecutor execService) {
        this(program, globalMem, program.hasDeterministicFunctions() ? new SimpleResultCache() : null,
                in, out, err, execService);
    }


    ExecutionContext(final Program program, final Object[] globalMem,
            @Nullable final ResultCache resultCache,
            @Nullable final InputStream in, @Nullable final PrintStream out, @Nullable final PrintStream err,
            @Nullable final VMExecutor execService) {
        this.code = program;
        this.in = in;
        this.out = out;
        this.err = err;
        this.execService = execService;
        this.stack = new SimpleStack(8);
        this.ip = 0;
        this.mem = new Object[program.getLocalMemSize()];
        this.globalMem = globalMem;
        this.resultCache = resultCache;
        isChildContext = false;
    }

    public VMExecutor.Suspendable<Object> getCallable() {
        return new VMExecutor.Suspendable<Object>() {

            @Override
            public Object call()
                    throws ExecutionException, InterruptedException, SuspendedException {
                suspendedAt = null;
                if (mathContext != null) {
                    Operator.MATH_CONTEXT.set(mathContext);
                }
                Instruction[] instructions = code.getInstructions();
                try {
                    while (!terminated) {
                        Instruction icode = instructions[ip];
                        ip += icode.execute(ExecutionContext.this);
                    }
                    if (!isStackEmpty()) {
                        Object[] results = popSyncStackVals(stack.size());
                        return results[results.length - 1];
                    } else {
                        return null;
                    }
                } catch (SuspendedException | InterruptedException e) {
                    throw e;
                } catch (ZExecutionException e) {
                    e.addZelFrame(new ZelFrame(code.getName(), code.getSource(),
                            code.getDebug()[ip].getRow()));
                    throw e;
                }
            }

            @Override
            public List<VMFuture<Object>> getSuspendedAt() {
                return ExecutionContext.this.suspendedAt;
            }
        };
    }




    /**
     * pops object out of stack
     *
     * @return Object
     */
    public Object popSyncStackVal() throws SuspendedException, ExecutionException {
        Object result = this.stack.peek();
        if (result instanceof VMFuture<?>) {
                final VMFuture<Object> resFut = (VMFuture<Object>) result;
                Either<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                if (resultStore != null) {
                    this.stack.remove();
                    return FutureBean.processResult(resultStore);
                } else {
                    suspend(resFut);
                    throw new IllegalThreadStateException();
                }
        } else {
            this.stack.remove();
            return result;
        }
    }

    public void syncStackVal() throws SuspendedException, ExecutionException {
        Object result = this.stack.peek();
        if (result instanceof VMFuture<?>) {
                final VMFuture<Object> resFut = (VMFuture<Object>) result;
                Either<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                if (resultStore == null) {
                    suspend(resFut);
                    throw new IllegalThreadStateException();
                } else {
                    this.stack.replaceFromTop(0, FutureBean.processResult(resultStore));
                }
        }
    }



    public Object[] popStackVals(final int nvals) {
        return stack.pop(nvals);
    }

    public Object popStackVal() {
        return stack.pop();
    }

    public int getNrStackVals() {
        return stack.size();
    }

    public Object[] popSyncStackVals(final int nvals) throws SuspendedException, ExecutionException {
        Object [] result = new Object[nvals];
        popSyncStackVals(result);
        return result;
    }

    private final Object[] tuple = new Object[2];

    @SuppressFBWarnings
    public Object[] tuple() {
        return tuple;
    }


    public void popSyncStackVals(final Object[] vals) throws SuspendedException, ExecutionException {
        final int l = vals.length;
        for (int i = 0, j = l - 1; i < l; i++, j--) {
            Object obj = stack.peekFromTop(i);
            if (obj instanceof VMFuture<?>) {
                    final VMFuture<Object> resFut = (VMFuture<Object>) obj;
                    Either<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                    if (resultStore != null) {
                        final Object processResult = FutureBean.processResult(resultStore);
                        stack.replaceFromTop(i, processResult);
                        vals[j] = processResult;
                    } else {
                        suspend(resFut);
                        throw new IllegalStateException();
                    }
            } else {
                vals[j] = obj;
            }
        }
        stack.removeFromTop(l);
    }


    public Object popFirstAvail(final int nr) throws SuspendedException {
        int nrErrors = 0;
        ExecutionException e = null;
        List<VMFuture<Object>> futures = null;
        for (int i = 0; i < nr; i++) {
            Object obj = stack.peekFromTop(i);
            if (obj instanceof VMFuture<?>) {
                final VMFuture<Object> resFut = (VMFuture<Object>) obj;
                Either<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                if (resultStore != null) {
                    if (resultStore.isLeft()) {
                        stack.removeFromTop(nr);
                        return resultStore.getLeft();
                    } else {
                        nrErrors++;
                        if (e == null) {
                            e = resultStore.getRight();
                        } else {
                            e = Throwables.chain(resultStore.getRight(), e);
                        }
                    }
                } else {
                    if (futures == null) {
                        futures = new ArrayList<>(nr);
                    }
                    futures.add(resFut);
                }
            } else {
                stack.removeFromTop(nr);
                return obj;
            }
        }
        if (nrErrors == nr) {
            if (e == null) {
                throw new IllegalStateException();
            } else {
                throw new RuntimeException(e);
            }
        }
        if (futures == null || futures.isEmpty()) {
            throw new IllegalStateException();
        }
        suspend(futures);
        throw new IllegalStateException();
    }

    public Object pop() {
        return this.stack.pop();
    }

    public void push(@Nullable final Object obj) {
        this.stack.push(obj);
    }

    public void pushAll(final Object[] objects) {
        this.stack.pushAll(objects);
    }

    public boolean isStackEmpty() {
        return this.stack.isEmpty();
    }

    public Object peek() {
        return this.stack.peek();
    }

    public Object peekFromTop(final int n) {
        return this.stack.peekFromTop(n);
    }

    public Object peekElemAfter(final Object elem) {
        return this.stack.peekElemAfter(elem);
    }

    public Object getFromPtr(final int ptr) {
        return this.stack.getFromPtr(ptr);
    }

    public ExecutionContext getSubProgramContext(final Program program, final Object[] parameters) {
        ExecutionContext ec;
        if (program.getExecType() == SYNC) {
            ec = new ExecutionContext(this, null, program);
        } else {
            ec = new ExecutionContext(this, this.execService, program);
        }
        System.arraycopy(parameters, 0, ec.mem, 0, parameters.length);
        return ec;
    }

    public ExecutionContext getSyncSubProgramContext(final Program program, final Object[] parameters) {
        ExecutionContext ec;
        ec = new ExecutionContext(this, null, program);
        System.arraycopy(parameters, 0, ec.mem, 0, parameters.length);
        return ec;
    }

    @Override
    public String toString() {
        return "ExecutionContext{" + "execService=" + execService + ",\nresultCache="
                + resultCache + ",\nmemory=" + Arrays.toString(mem)
                + ",\nlocalSymbolTable=" + code.getLocalSymbolTable()
                + ",\nglobalMem=" + Arrays.toString(globalMem)
                + ",\nglobalSymbolTable=" + code.getGlobalSymbolTable()
                + ",\ncode=" + code + ", ip=" + ip + ", terminated=" + terminated
                + ",\nstack=" + stack + ", in=" + in
                + ",\nout=" + out + ", err=" + err + '}';
    }

    public boolean isChildContext() {
        return isChildContext;
    }



}
