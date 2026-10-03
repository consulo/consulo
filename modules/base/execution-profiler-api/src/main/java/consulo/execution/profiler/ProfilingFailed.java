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
package consulo.execution.profiler;

import consulo.localize.LocalizeValue;

/**
 * Profiling ended without data.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilingFailed implements ProfilerState {
    private final LocalizeValue myMessage;

    public ProfilingFailed(LocalizeValue message) {
        myMessage = message;
    }

    /**
     * @return why profiling failed, shown to the user
     */
    public LocalizeValue getMessage() {
        return myMessage;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
