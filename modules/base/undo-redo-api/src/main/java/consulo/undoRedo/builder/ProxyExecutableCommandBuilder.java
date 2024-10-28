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
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-10-28
 */
public class ProxyExecutableCommandBuilder<R, THIS extends ExecutableCommandBuilder<R, THIS>, THAT extends ExecutableCommandBuilder<R, THAT>>
    extends ProxyCommandBuilder<THIS, THAT> implements ExecutableCommandBuilder<R, THIS> {

    @Nonnull
    private final Consumer<Runnable> myRunner;

    public ProxyExecutableCommandBuilder(THAT subBuilder, @RequiredUIAccess @Nonnull Consumer<Runnable> runner) {
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
