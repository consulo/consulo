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
package consulo.undoRedo.internal.builder;

import consulo.document.util.DocumentUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.builder.ExecutableCommandBuilder;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-11-10
 */
interface WrappableExecutableCommandBuilder<R, THIS extends WrappableExecutableCommandBuilder<R, THIS>>
    extends ExecutableCommandBuilder<R, THIS>, WrappableCommandBuilder<THIS> {

    @Override
    default THIS inLater() {
        return outerWrap(runnable -> getApplication().invokeLater(runnable));
    }

    @Override
    default THIS inBulkUpdate() {
        return outerWrap(runnable -> DocumentUtil.executeInBulk(build(EmptyRunnable.INSTANCE).document(), true, runnable));
    }

    @Override
    default THIS inGlobalUndoAction() {
        return innerWrap(runnable -> {
            getCommandProcessor().markCurrentCommandAsGlobal(build(EmptyRunnable.INSTANCE).project());
            runnable.run();
        });
    }

    @Override
    default THIS inUndoTransparentAction() {
        return outerWrap(runnable -> getCommandProcessor().runUndoTransparentAction(runnable));
    }

    @Override
    default THIS inWriteAction() {
        return innerWrap(runnable -> getApplication().runWriteAction(runnable));
    }

    @SuppressWarnings("unchecked")
    default THIS outerWrap(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new OuterWrapper<R, THIS, THIS>((THIS)this, runner);
    }

    @SuppressWarnings("unchecked")
    default THIS innerWrap(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new InnerWrapper<R, THIS, THIS>((THIS)this, runner);
    }

    public class OuterWrapper<R, THIS extends WrappableExecutableCommandBuilder<R, THIS>, THAT extends WrappableExecutableCommandBuilder<R, THAT>>
        extends BaseCommandBuilderWrapper<THIS, THAT> implements WrappableExecutableCommandBuilder<R, THIS> {

        @Nonnull
        protected final Consumer<Runnable> myRunner;

        public OuterWrapper(THAT subBuilder, @RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
            super(subBuilder);
            myRunner = runner;
        }

        @Override
        public ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
            SimpleReference<ExecutionResult<R>> result = SimpleReference.create();
            myRunner.accept(() -> result.set(mySubBuilder.execute(executable)));
            return result.get();
        }
    }

    public class InnerWrapper<R, THIS extends WrappableExecutableCommandBuilder<R, THIS>, THAT extends WrappableExecutableCommandBuilder<R, THAT>>
        extends OuterWrapper<R, THIS, THAT> {

        public InnerWrapper(THAT subBuilder, @RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
            super(subBuilder, runner);
        }

        @Override
        public ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
            return mySubBuilder.execute(() -> {
                SimpleReference<ExecutionResult<R>> result = SimpleReference.create();
                myRunner.accept(() -> result.set(ExecutionResult.execute(executable)));
                return result.get().get();
            });
        }
    }
}
