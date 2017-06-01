
package org.spf4j.zel.instr;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;

public final class SyncAsyncCallable implements Callable<Object> {

    private final ExecutionContext nctx;

    public SyncAsyncCallable(final ExecutionContext nctx) {
        this.nctx = nctx;
    }

    @Override
    public Object call() throws ExecutionException, InterruptedException  {
        return nctx.executeSyncOrAsync();
    }

    @Override
    public String toString() {
        return "SyncAsyncCallable{" + "nctx=" + nctx + '}';
    }

}
