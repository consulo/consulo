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
package consulo.ui.impl.chart;

import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ChartTime {
    private ChartTime() {
    }

    public static long toMicros(Instant instant) {
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000L), instant.getNano() / 1_000);
    }

    public static long toNanos(Instant instant) {
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L), instant.getNano());
    }

    public static Instant fromMicros(double micros) {
        long whole = (long) Math.floor(micros);
        return Instant.ofEpochSecond(Math.floorDiv(whole, 1_000_000L), Math.floorMod(whole, 1_000_000L) * 1_000L);
    }
}
