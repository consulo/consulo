/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.application.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.function.Processor;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author cdr
 */
public abstract class ReadActionProcessor<T> implements Processor<T> {
    @Override
    public boolean process(final T t) {
        return Application.get().runReadAction((Supplier<Boolean>)() -> processInReadAction(t));
    }

    @RequiredReadAction
    public abstract boolean processInReadAction(T t);

    @Nonnull
    public static <T> Processor<T> wrapInReadAction(@Nonnull Predicate<T> processor) {
        return t -> Application.get().runReadAction((Supplier<Boolean>)() -> processor.test(t));
    }
}
