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

import consulo.undoRedo.builder.ThrowableRunnableCommandBuilder;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-11-10
 */
public interface WrappableThrowableRunnableCommandBuilder<R, E extends Throwable, THIS extends WrappableThrowableRunnableCommandBuilder<R, E, THIS>>
    extends WrappableExecutableCommandBuilder<R, THIS>, ThrowableRunnableCommandBuilder<R, E, THIS> {

    @Override
    @SuppressWarnings("unchecked")
    default THIS outerWrap(@Nonnull Consumer<Runnable> runner) {
        return (THIS)new OuterWrapper<R, E, THIS, THIS>(
            (THIS)this,
            runner,
            getExceptionClass()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    default THIS innerWrap(@Nonnull Consumer<Runnable> runner) {
        return (THIS)new InnerWrapper<R, E, THIS, THIS>(
            (THIS)this,
            runner,
            getExceptionClass()
        );
    }

    class OuterWrapper<
            R,
            E extends Throwable,
            THIS extends WrappableThrowableRunnableCommandBuilder<R, E, THIS>,
            THAT extends WrappableExecutableCommandBuilder<R, THAT>
        >
        extends WrappableExecutableCommandBuilder.OuterWrapper<R, THIS, THAT>
        implements WrappableThrowableRunnableCommandBuilder<R, E, THIS> {

        private final Class<E> myExceptionClass;

        public OuterWrapper(THAT subBuilder, @Nonnull Consumer<Runnable> runner, Class<E> exceptionClass) {
            super(subBuilder, runner);
            myExceptionClass = exceptionClass;
        }

        @Override
        public Class<E> getExceptionClass() {
            return myExceptionClass;
        }
    }

    class InnerWrapper<
            R,
            E extends Throwable,
            THIS extends WrappableThrowableRunnableCommandBuilder<R, E, THIS>,
            THAT extends WrappableExecutableCommandBuilder<R, THAT>
        >
        extends WrappableExecutableCommandBuilder.InnerWrapper<R, THIS, THAT>
        implements WrappableThrowableRunnableCommandBuilder<R, E, THIS> {

        private final Class<E> myExceptionClass;

        public InnerWrapper(THAT subBuilder, @Nonnull Consumer<Runnable> runner, Class<E> exceptionClass) {
            super(subBuilder, runner);
            myExceptionClass = exceptionClass;
        }

        @Override
        public Class<E> getExceptionClass() {
            return myExceptionClass;
        }
    }
}
