/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.application;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.function.ThrowableComputable;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;

@Deprecated
@DeprecationInfo("View WriteLock")
public final class WriteAction {
    public static void runAndWait(@RequiredWriteAction Runnable runnable) {
        run(runnable::run);
    }

    public static <E extends Throwable> void run(ThrowableRunnable<E> action) throws E {
        Application application = Application.get();
        application.runWriteAction((ThrowableSupplier<Object, E>) () -> {
            action.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T compute(ThrowableComputable<T, E> action) throws E {
        Application application = Application.get();
        return application.runWriteAction(action);
    }

    public static void runLater(@RequiredWriteAction Runnable runnable) {
        Application application = Application.get();
        ApplicationConcurrency concurrency = application.getInstance(ApplicationConcurrency.class);
        concurrency.executor().execute(() -> application.runWriteAction(runnable));
    }
}
