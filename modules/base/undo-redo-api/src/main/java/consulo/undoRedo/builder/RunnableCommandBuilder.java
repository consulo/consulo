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

import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static consulo.undoRedo.builder.ThrowableRunnableCommandBuilder.ProxyThrowableRunnableCommandBuilder;

/**
 * @author UNV
 * @since 2024-10-28
 */
public interface RunnableCommandBuilder<R, THIS extends RunnableCommandBuilder<R, THIS>> extends ExecutableCommandBuilder<R, THIS> {
    @SuppressWarnings("unchecked")
    default public <E extends Exception, PRX extends ProxyThrowableRunnableCommandBuilder<R, E, PRX, THIS>>
    ThrowableRunnableCommandBuilder<R, E, ? extends ThrowableRunnableCommandBuilder> canThrow(Class<E> exceptionClass) {
        return new ProxyThrowableRunnableCommandBuilder<R, E, PRX, THIS>((THIS)this, Runnable::run, exceptionClass);
    }

    default void run(@RequiredUIAccess @Nonnull Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        }).checkException();
    }

    default R compute(@RequiredUIAccess @Nonnull Supplier<R> supplier) {
        return execute(supplier::get).get();
    }

    @Override
    @SuppressWarnings("unchecked")
    default THIS proxy(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new ProxyRunnableCommandBuilder<R, THIS, THIS>((THIS)this, runner);
    }

    class ProxyRunnableCommandBuilder<R, THIS extends RunnableCommandBuilder<R, THIS>, THAT extends ExecutableCommandBuilder<R, THAT>>
        extends ProxyExecutableCommandBuilder<R, THIS, THAT> implements RunnableCommandBuilder<R, THIS> {

        public ProxyRunnableCommandBuilder(THAT subBuilder, @Nonnull Consumer<Runnable> runner) {
            super(subBuilder, runner);
        }
    }
}
