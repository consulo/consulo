/*
 * Copyright 2013-2024 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.undoRedo.builder;

import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.document.util.DocumentUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-10-28
 */
public interface ExecutableCommandBuilder<R, THIS extends ExecutableCommandBuilder<R, THIS>> extends CommandBuilder<THIS> {
    @SuppressWarnings("unchecked")
    default THIS inLater() {
        return proxy(runnable -> Application.get().invokeLater(runnable));
    }

    @SuppressWarnings("unchecked")
    default THIS inLaterIf(boolean inLater) {
        return inLater ? inLater() : (THIS)this;
    }

    @SuppressWarnings("unchecked")
    default THIS inBulkUpdate() {
        return proxy(runnable -> DocumentUtil.executeInBulk(build(EmptyRunnable.INSTANCE).document(), true, runnable));
    }

    @SuppressWarnings("unchecked")
    default THIS inBulkUpdateIf(boolean inBulkUpdate) {
        return inBulkUpdate ? inBulkUpdate() : (THIS)this;
    }

    THIS inGlobalUndoAction();

    @SuppressWarnings("unchecked")
    default THIS inGlobalUndoActionIf(boolean globalUndoAction) {
        return globalUndoAction ? inGlobalUndoAction() : (THIS)this;
    }

    @SuppressWarnings("unchecked")
    default THIS inUndoTransparentAction() {
        return proxy(runnable -> CommandProcessor.getInstance().runUndoTransparentAction(runnable));
    }

    @SuppressWarnings("unchecked")
    default THIS inWriteAction() {
        return proxy(runnable -> Application.get().runWriteAction(runnable));
    }

    @SuppressWarnings("unchecked")
    default THIS inWriteActionIf(boolean writeAction) {
        return writeAction ? inWriteAction() : (THIS)this;
    }

    @SuppressWarnings("unchecked")
    default THIS proxy(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new ProxyExecutableCommandBuilder<R, THIS, THIS>((THIS)this, runner);
    }

    ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable);

    static class ExecutionResult<R> {
        private final R myResult;
        private final Throwable myThrowable;

        private ExecutionResult(R result, Throwable throwable) {
            myResult = result;
            myThrowable = throwable;
        }

        public ExecutionResult(R result) {
            this(result, null);
        }

        public ExecutionResult(Throwable throwable) {
            this(null, throwable);
        }

        public static <R> ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
            try {
                return new ExecutionResult<>(executable.get());
            }
            catch (ProcessCanceledException | Error e) {
                // ProcessCanceledException may occur from time to time and it shouldn't be catched
                throw e;
            }
            catch (Throwable throwable) {
                return new ExecutionResult<>(throwable);
            }
        }

        public R get() {
            return get(null);
        }

        public <E extends Throwable> R get(@Nullable Class<E> exceptionClass) throws E {
            checkException(exceptionClass);
            return myResult;
        }

        public void checkException() {
            checkException(null);
        }

        @SuppressWarnings("unchecked")
        public <E extends Throwable> void checkException(@Nullable Class<E> exceptionClass) throws E {
            if (hasException()) {
                if (myThrowable instanceof RuntimeException re) {
                    throw re;
                }

                if (myThrowable instanceof Error e) {
                    throw e;
                }

                if (exceptionClass != null && exceptionClass.isInstance(myThrowable)) {
                    throw (E)myThrowable;
                }

                throw new RuntimeException(myThrowable);
            }
        }

        public boolean hasException() {
            return myThrowable != null;
        }

        public Throwable getThrowable() {
            return myThrowable;
        }
    }
}
