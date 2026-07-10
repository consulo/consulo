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
package consulo.util.concurrent.coroutine.internal;

import consulo.util.lang.ControlFlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-01-29
 */
public class DefaultExceptionHandler implements Consumer<Throwable> {
    public static final DefaultExceptionHandler INSTANCE = new DefaultExceptionHandler();

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    @Override
    public void accept(Throwable throwable) {
        // cancellation (e.g. ProcessCanceledException) is normal control flow, not an error to log
        if (isControlFlow(throwable)) {
            return;
        }
        log.error("", throwable);
    }

    private static boolean isControlFlow(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (t instanceof ControlFlowException || t instanceof CancellationException) {
                return true;
            }
        }
        return false;
    }
}
