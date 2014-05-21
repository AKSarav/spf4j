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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.spf4j.base.Pair;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.instr.LODAX;
import org.spf4j.zel.instr.LODAXF;
import org.spf4j.zel.instr.LODX;
import org.spf4j.zel.instr.LODXF;
import org.spf4j.zel.instr.var.ARRAY;
import org.spf4j.zel.instr.var.INT;
import org.spf4j.zel.instr.var.LOG;
import org.spf4j.zel.instr.var.MAX;
import org.spf4j.zel.instr.var.MIN;
import org.spf4j.zel.instr.var.OUT;
import org.spf4j.zel.instr.var.RANDOM;
import org.spf4j.zel.instr.var.SQRT;
import org.spf4j.zel.vm.gen.ParseException;
import org.spf4j.zel.vm.gen.TokenMgrError;
import org.spf4j.zel.vm.gen.ZCompiler;

/**
 * <p>
 * Title: Program</p>
 *
 * @author zoly
 * @version 1.0
 *
 * This is a Turing machine a Program will always be pretty much an array of operations.
 */
@Immutable
public final class Program implements Serializable {

    static final long serialVersionUID = 748365748433474932L;

    public enum Type {
        DETERMINISTIC, NONDETERMINISTIC
    };

    public enum ExecutionType {
        SYNC,
        ASYNC
    }

    private final Type type;
    private final ExecutionType execType;
    private final int id; // program ID, unique ID identifying the program

    private final Instruction[] instructions;
    private final String[] parameterNames;
    private final boolean hasDeterministicFunctions;
    private final Object[] globalMem;
    private final int localMemSize;
    private final Map<String, Integer> localSymbolTable;


    Program(final Map<String, Integer> globalTable, final Object[] globalMem, 
            @Nonnull final Instruction[] objs, @Nonnegative final int start,
            @Nonnegative final int end, final Type progType, final ExecutionType execType,
            final boolean hasDeterministicFunctions, final String... parameterNames) throws CompileException {
        this(globalTable, globalMem, Collections.EMPTY_MAP,
                objs, start, end, progType, execType, hasDeterministicFunctions, parameterNames);
    
    }
    
    Program(final Map<String, Integer> globalTable, final Object[] globalMem, final Map<String, Integer> localTable,
            @Nonnull final Instruction[] objs, @Nonnegative final int start,
            @Nonnegative final int end, final Type progType, final ExecutionType execType,
            final boolean hasDeterministicFunctions, final String... parameterNames) throws CompileException {
        this.globalMem = globalMem;
        int length = end - start;
        this.instructions = new Instruction[length];
        System.arraycopy(objs, start, instructions, 0, length);
        this.type = progType;
        this.id = ProgramBuilder.generateID();
        this.parameterNames = parameterNames;
        this.execType = execType;
        this.hasDeterministicFunctions = hasDeterministicFunctions;
        this.localSymbolTable = buildLocalSymTable(parameterNames, length, globalTable, localTable);
        this.localMemSize = localSymbolTable.size();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            { "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", "ITC_INHERITANCE_TYPE_CHECKING" })
    private Map<String, Integer> buildLocalSymTable(final String[] parameterNames1,
            final int length, final Map<String, Integer> globalTable,
            Map<String, Integer> addTo) throws CompileException {
        final int addToSize = addTo.size();
        Map<String, Integer> symbolTable = new HashMap<String, Integer>(addToSize + parameterNames1.length);
        symbolTable.putAll(addTo);
        int i = addToSize;
        for (String param : parameterNames1) {
            Integer existing = symbolTable.put(param, i++);
            if (existing != null) {
                throw new CompileException("Duplicate parameter defined: " + param);
            }
        }
        for (int j = 0; j < length; j++) {
            Instruction code = instructions[j];
            if (code instanceof LODX) {
                String ref = ((LODX) code).getSymbol();
                Integer idxr = symbolTable.get(ref);
                Address adr;
                if (idxr == null) {
                    idxr = globalTable.get(ref);
                    if (idxr == null) {
                        throw new CompileException("undefined variable: " + ref);
                    } else {
                        adr = new Address(idxr, Address.Scope.GLOBAL);
                    }
                } else {
                    adr = new Address(idxr, Address.Scope.LOCAL);
                }
                instructions[j] = new LODXF(adr);
            } else if (code instanceof LODAX) {
                String ref = ((LODAX) code).getSymbol();
                Integer idxr = symbolTable.get(ref);
                Address adr;
                if (idxr == null) {
                    idxr = globalTable.get(ref);
                    if (idxr == null) {
                        idxr = i++;
                        symbolTable.put(ref, idxr);
                        adr = new Address(idxr, Address.Scope.LOCAL);
                    } else {
                        adr = new Address(idxr, Address.Scope.GLOBAL);
                    }
                } else {
                    adr = new Address(idxr, Address.Scope.LOCAL);
                }
                instructions[j] = new LODAXF(adr);
            }
        }
        return symbolTable;
    }

    public Map<String, Integer> getLocalSymbolTable() {
        return localSymbolTable;
    }
    
    public int getLocalMemSize() {
       return localMemSize;
    }

    @Override
    @CheckReturnValue
    public boolean equals(@Nullable final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Program other = (Program) obj;
        return (this.id == other.id);
    }

    @Override
    @CheckReturnValue
    public int hashCode() {
        return this.id;
    }

    public boolean hasDeterministicFunctions() {
        return hasDeterministicFunctions;
    }

    /**
     * @return the instructions
     */
    @CheckReturnValue
    public Object get(final int i) {
        return instructions[i];
    }

    @CheckReturnValue
    Object[] toArray() {
        return instructions.clone();
    }

    @CheckReturnValue
    public int size() {
        return instructions.length;
    }

    public ExecutionType getExecType() {
        return execType;
    }

    public static Program compile(@Nonnull final String zExpr, @Nonnull final String... varNames)
            throws CompileException {

        CompileContext cc = new CompileContext(ZEL_GLOBAL_FUNC.copy());
        try {
            ZCompiler.compile(zExpr, cc);
        } catch (TokenMgrError err) {
            throw new CompileException(err);
        } catch (ParseException ex) {
            throw new CompileException(ex);
        }
        return cc.getProgramBuilder().toProgram(varNames);
    }
    
    
    static Program compile(@Nonnull final String zExpr, final Map<String, Integer> localTable,
            @Nonnull final String... varNames)
            throws CompileException {

        CompileContext cc = new CompileContext(ZEL_GLOBAL_FUNC.copy());
        try {
            ZCompiler.compile(zExpr, cc);
        } catch (TokenMgrError err) {
            throw new CompileException(err);
        } catch (ParseException ex) {
            throw new CompileException(ex);
        }
        return cc.getProgramBuilder().toProgram(varNames, localTable);
    }

    public Object execute() throws ZExecutionException, InterruptedException {
        return execute(System.in, System.out, System.err);
    }

    public Object execute(final Object... args) throws ZExecutionException, InterruptedException {
        return execute(System.in, System.out, System.err, args);
    }

    public Object execute(@Nonnull final ExecutorService execService,
            final Object... args) throws ZExecutionException, InterruptedException {
        return execute(new VMExecutor(execService), System.in, System.out, System.err, args);
    }
    
    public Object executeSingleThreaded(final Object... args) throws ZExecutionException, InterruptedException {
        return execute(null, System.in, System.out, System.err, args);
    }


    private static final MemoryBuilder ZEL_GLOBAL_FUNC;

    static {
        ZEL_GLOBAL_FUNC = new MemoryBuilder();
        ZEL_GLOBAL_FUNC.addSymbol("out", OUT.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("sqrt", SQRT.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("int", INT.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("log", LOG.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("log10", LOG.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("min", MIN.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("max", MAX.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("array", ARRAY.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("random", RANDOM.INSTANCE);
        ZEL_GLOBAL_FUNC.addSymbol("channel", new Channel.Factory());
        ZEL_GLOBAL_FUNC.addSymbol("EOF", Channel.EOF);
    }

    public Object execute(@Nullable final VMExecutor execService,
            @Nullable final InputStream in,
            @Nullable final PrintStream out,
            @Nullable final PrintStream err,
            final Object... args)
            throws ZExecutionException, InterruptedException {
        final ExecutionContext ectx = new ExecutionContext(this, globalMem, in, out, err, execService);
        System.arraycopy(args, 0, ectx.mem, 0, args.length);
        try {
            return execute(ectx);
        } finally {
            if (err != null) {
                err.flush();
            }
            if (out != null) {
                out.flush();
            }
        }
    }
    
    public Pair<Object, ExecutionContext> executeX(@Nullable final VMExecutor execService,
            @Nullable final InputStream in,
            @Nullable final PrintStream out,
            @Nullable final PrintStream err,
            final Object... args)
            throws ZExecutionException, InterruptedException {
        final ExecutionContext ectx = new ExecutionContext(this, globalMem, in, out, err, execService);
        System.arraycopy(args, 0, ectx.mem, 0, args.length);
        try {
            return Pair.of(execute(ectx), ectx);
        } finally {
            if (err != null) {
                err.flush();
            }
            if (out != null) {
                out.flush();
            }
        }
    }

    public static Object executeSyncOrAsync(@Nonnull final ExecutionContext ectx)
            throws ZExecutionException, InterruptedException {
        final VMExecutor.Suspendable<Object> execution = ectx.getCallable();
        if (ectx.execService != null && ectx.code.getExecType() == ExecutionType.ASYNC) {
            if (ectx.isChildContext()) {
                return ectx.execService.submitInternal(VMExecutor.synchronize(execution));
            } else {
                return ectx.execService.submit(VMExecutor.synchronize(execution));
            }
        } else {
            try {
                return execution.call();
            } catch (SuspendedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static Object executeAsync(@Nonnull final ExecutionContext ectx)
            throws ZExecutionException, InterruptedException {
        final VMExecutor.Suspendable<Object> execution = ectx.getCallable();
        if (ectx.execService != null) {
            if (ectx.isChildContext()) {
                return ectx.execService.submitInternal(VMExecutor.synchronize(execution));
            } else {
                return ectx.execService.submit(VMExecutor.synchronize(execution));
            }
        } else {
            try {
                return execution.call();
            } catch (SuspendedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static Object executeSync(@Nonnull final ExecutionContext ectx) throws
            ZExecutionException, InterruptedException {
        try {
            return ectx.getCallable().call();
        } catch (SuspendedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Object execute(@Nonnull final ExecutionContext ectx)
            throws ZExecutionException, InterruptedException {
        Object result = executeSyncOrAsync(ectx);
        if (result instanceof Future) {
            try {
                return ((Future<Object>) result).get();
            } catch (ExecutionException ex) {
                throw new ZExecutionException(ex);
            }
        } else {
            return result;
        }
    }

    public Object execute(@Nonnull final InputStream in,
            @Nonnull final PrintStream out, @Nonnull final PrintStream err, final Object... args)
            throws ZExecutionException, InterruptedException {
        if (execType == ExecutionType.SYNC) {
            return execute((VMExecutor) null, in, out, err, args);
        } else {
            return execute(VMExecutor.Lazy.DEFAULT, in, out, err, args);
        }
    }

    /**
     * get a value of a variable from memory, this function is syntax safe
     *
     * @param mem Map
     * @param name String
     * @throws Exception
     * @return Object
     */
    public static Object getValue(@Nonnull final java.util.Map mem, @Nonnull final String name)
            throws CompileException, ZExecutionException, InterruptedException, ExecutionException {
        return Program.compile(name + ";").execute(mem);
    }

    /**
     * Load a value into memory Have to go through the VM so that the assignement is acurate
     *
     * @param mem
     * @param name String
     * @param value Object
     * @throws net.sf.zel.vm.ParseException
     * @throws net.sf.zel.vm.ZExecutionException
     * @throws java.lang.InterruptedException
     */
    public static void addValue(@Nonnull final java.util.Map mem, @Nonnull final String name,
            final Object value)
            throws CompileException, ZExecutionException, InterruptedException, ExecutionException {
        Program.compile(name + "=" + value + ";").execute(mem);
    }

    /**
     * build indentation string
     *
     * @param indent
     * @return
     */
    @CheckReturnValue
    public static String strIndent(@Nonnegative final int indent) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    /**
     * Output Core, in hierarchical tab indented mode
     *
     * @param name
     * @param mem
     * @param indent
     * @param maxIndent
     * @return
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public static String dumpCore(final String name, final Object mem, final int indent, final int maxIndent) {
        if (mem == null) {
            return "";
        }
        if (maxIndent > 0 && indent > maxIndent) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        if (mem instanceof java.util.Map) {
            result.append(strIndent(indent)).append(name).append('\n');
            for (Map.Entry<Object, Object> elem : ((Map<Object, Object>) mem).entrySet()) {
                result.append(dumpCore(elem.getKey().toString(), elem.getValue(), indent + 1, maxIndent));
            }
        } else {
            result.append(strIndent(indent)).append(name).append('=').append(mem).append('\n');
        }
        return result.toString();
    }

    /**
     * *
     * This allows to run ZEL in an interactive mode
     *
     * @param args
     */
    public static void main(final String[] args)
            throws IOException, ZExecutionException, InterruptedException {
        System.out.println("ZEL Shell");
        boolean terminated = false;
        Map<String, Integer> localSymTable = Collections.EMPTY_MAP;
        Object [] mem = new Object [] {};
        InputStreamReader inp = new InputStreamReader(System.in, Charsets.UTF_8);
        BufferedReader br = new BufferedReader(inp);
        while (!terminated) {
            System.out.print("zel>");
            String line = br.readLine();
            if (line != null) {
                if (line.toUpperCase().startsWith("QUIT")) {
                    terminated = true;
                } else {
                    try {
                        final Program prog = Program.compile(line, localSymTable);
                        localSymTable = prog.getLocalSymbolTable();
                        Pair<Object, ExecutionContext> res = prog.executeX(
                                VMExecutor.Lazy.DEFAULT, System.in, System.out, System.err, mem);
                        System.out.println("result>" + res.getFirst());
                        mem = res.getSecond().mem;
                    } catch (CompileException ex) {
                        System.out.println("Syntax Error: " + Throwables.getStackTraceAsString(ex));
                    } catch (ZExecutionException ex) {
                        System.out.println("Execution Error: " + Throwables.getStackTraceAsString(ex));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Program: \n");
        for (int i = 0; i < instructions.length; i++) {
            Object obj = instructions[i];
            result.append(Strings.padEnd(Integer.toString(i), 8, ' '));
            result.append(':');
            result.append(obj);
            result.append('\n');
        }
        result.append("execType = " + this.execType + "\n");
        result.append("type = " + this.type + "\n");
        return result.toString();
    }

    /**
     * @return the type
     */
    public Program.Type getType() {
        return type;
    }

    String[] getParameterNames() {
        return parameterNames;
    }

    public boolean contains(final Class<? extends Instruction> instr) {
        Boolean res = itterate(new Function<Object, Boolean>() {
            @Override
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("TBP_TRISTATE_BOOLEAN_PATTERN")
            public Boolean apply(final Object input) {
                if (input.getClass() == instr) {
                    return Boolean.TRUE;
                }
                return null;
            }
        });
        if (res == null) {
            return false;
        }
        return res;
    }

    @Nullable
    public <T> T itterate(final Function<Object, T> func) {
        for (Instruction code : instructions) {
            T res = func.apply(code);
            if (res != null) {
                return res;
            }
            for (Object param : code.getParameters()) {
                res = func.apply(param);
                if (res != null) {
                   return res;
                }
                if (param instanceof Program) {
                    res = ((Program) param).itterate(func);
                }
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    Instruction[] getInstructions() {
        return instructions;
    }

}
