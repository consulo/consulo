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
package consulo.execution.profiler.impl.internal.view;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerFormat {
    private ProfilerFormat() {
    }

    public static double percent(long part, long total) {
        return total <= 0 ? 0 : part * 100.0 / total;
    }

    public static String formatPercent(@Nullable Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.1f%%", value);
    }

    public static String formatCount(@Nullable Long value) {
        return value == null ? "" : String.format(Locale.ROOT, "%,d", value);
    }
}
