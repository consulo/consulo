// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.uast.internal;

import consulo.language.uast.util.UastImplementationUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Collects diagnostic messages emitted through {@link Logger} while a body runs on the current thread,
 * so that a failed UAST conversion can be reported together with the log of what was tried.
 */
public class ThreadLocalTroubleCollector {
    private final ThreadLocal<StringBuilder> myCollected = new ThreadLocal<>();

    private final Logger myLogger = new Logger();

    public <T extends @Nullable Object> Pair<T, String> withCollectingInfo(Supplier<T> body) {
        StringBuilder out = new StringBuilder();
        T result = UastImplementationUtil.withValue(myCollected, out, body);
        return Pair.create(result, out.toString());
    }

    public Logger getLogger() {
        return myLogger;
    }

    public class Logger {
        public void log(Supplier<String> message) {
            StringBuilder stringBuilder = myCollected.get();
            if (stringBuilder == null) {
                return;
            }
            stringBuilder.append(message.get()).append('\n');
        }

        public <T> @Nullable T logAndNull(Supplier<String> message) {
            log(message);
            return null;
        }
    }
}
