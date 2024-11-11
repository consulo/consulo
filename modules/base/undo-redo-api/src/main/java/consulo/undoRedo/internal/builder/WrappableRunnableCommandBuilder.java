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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.builder.*;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-11-10
 */
public interface WrappableRunnableCommandBuilder<R, THIS extends WrappableRunnableCommandBuilder<R, THIS>>
    extends WrappableExecutableCommandBuilder<R, THIS>, RunnableCommandBuilder<R, THIS> {

    @Override
    @SuppressWarnings("unchecked")
    default <E extends Exception, PRX extends ThrowableRunnableCommandBuilder<R, E, PRX>>
    PRX canThrow(Class<E> exceptionClass) {
        return (PRX)canThrow0(exceptionClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    default THIS outerWrap(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new OuterWrapper<R, THIS, THIS>((THIS)this, runner);
    }

    @Override
    @SuppressWarnings("unchecked")
    default THIS innerWrap(@RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
        return (THIS)new InnerWrapper<R, THIS, THIS>((THIS)this, runner);
    }

    @SuppressWarnings("unchecked")
    default <E extends Exception, PRX extends WrappableThrowableRunnableCommandBuilder<R, E, PRX>>
    PRX canThrow0(Class<E> exceptionClass) {
        return (PRX)new WrappableThrowableRunnableCommandBuilder.OuterWrapper<R, E, PRX, THIS>((THIS)this, Runnable::run, exceptionClass);
    }

    class OuterWrapper<R, THIS extends WrappableRunnableCommandBuilder<R, THIS>, THAT extends WrappableExecutableCommandBuilder<R, THAT>>
        extends WrappableExecutableCommandBuilder.OuterWrapper<R, THIS, THAT> implements WrappableRunnableCommandBuilder<R, THIS> {

        public OuterWrapper(THAT subBuilder, @Nonnull Consumer<Runnable> runner) {
            super(subBuilder, runner);
        }
    }

    class InnerWrapper<R, THIS extends WrappableRunnableCommandBuilder<R, THIS>, THAT extends WrappableExecutableCommandBuilder<R, THAT>>
        extends WrappableExecutableCommandBuilder.InnerWrapper<R, THIS, THAT> implements WrappableRunnableCommandBuilder<R, THIS> {

        public InnerWrapper(THAT subBuilder, @Nonnull Consumer<Runnable> runner) {
            super(subBuilder, runner);
        }
    }
}
