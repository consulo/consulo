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
package consulo.component.impl.internal.inject;

import consulo.util.lang.ControlFlowException;

/**
 * Thrown when a service whose state loads asynchronously is requested as a plain constructor parameter.
 * Creating it runs a coroutine, so it can only be reached through a provider.
 * <p>
 * Marked as a {@link ControlFlowException} so that the catch-all in
 * {@link BaseComponentAdapter#getComponentInstance} rethrows it instead of logging it - this is a wiring
 * mistake the caller has to see, not a runtime failure to report and continue past.
 *
 * @author VISTALL
 * @since 2026-07-29
 */
public class AsyncInjectionNotSupportedException extends PicoInitializationException implements ControlFlowException {
    public AsyncInjectionNotSupportedException(Class<?> requestedBy, Class<?> target) {
        super("Async state component " +
            target.getName() +
            " can not be injected directly into " +
            requestedBy.getName() +
            " - use Provider<" +
            target.getSimpleName() +
            "> or ProviderAsync<" +
            target.getSimpleName() +
            "> instead");
    }
}
