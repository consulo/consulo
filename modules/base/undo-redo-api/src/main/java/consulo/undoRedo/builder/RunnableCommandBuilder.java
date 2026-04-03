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
import consulo.util.lang.function.ThrowableSupplier;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author UNV
 * @since 2024-10-28
 */
public interface RunnableCommandBuilder<R, THIS extends RunnableCommandBuilder<R, THIS>> extends ExecutableCommandBuilder<R, THIS> {
    <E extends Exception, PRX extends ThrowableRunnableCommandBuilder<R, E, PRX>>
    PRX canThrow(Class<E> exceptionClass);

    @SuppressWarnings("NullAway")
    default void run(@RequiredUIAccess Runnable runnable) {
        execute(() -> {
            runnable.run();
            // Technical null. Disabling NullAway checks here
            return null;
        }).checkException();
    }

    default R compute(@RequiredUIAccess Supplier<R> supplier) {
        return execute(supplier::get).get();
    }
}
