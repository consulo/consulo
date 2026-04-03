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
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-10-28
 */
public interface ThrowableRunnableCommandBuilder<R extends @Nullable Object, E extends Throwable, THIS extends ThrowableRunnableCommandBuilder<R, E, THIS>>
    extends ExecutableCommandBuilder<R, THIS> {

    Class<E> getExceptionClass();

    @SuppressWarnings("NullAway")
    default void run(@RequiredUIAccess ThrowableRunnable<E> runnable) throws E {
        execute(() -> {
            runnable.run();
            // This is a technical null. If we're here R and it's nullability has no meaning. We're not returning
            // any result to the user anyway. But we cannot describe this to static validator, so disabling NullAway here.
            return null;
        });
    }

    default R compute(@RequiredUIAccess ThrowableSupplier<R, E> supplier) throws E {
        return execute(supplier).get(getExceptionClass());
    }
}
