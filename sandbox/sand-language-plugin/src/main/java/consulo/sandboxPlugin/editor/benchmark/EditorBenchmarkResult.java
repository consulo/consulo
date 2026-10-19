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
package consulo.sandboxPlugin.editor.benchmark;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public record EditorBenchmarkResult(
    String scenario,
    int runs,
    double cpuMedianMs,
    double cpuP90Ms,
    double cpuMaxMs,
    double wallMedianMs,
    int paintedPercent
) {
    public static EditorBenchmarkResult of(String scenario, long[] cpuNanos, long[] wallNanos, long[] paintedPixels, long viewportPixels) {
        long[] cpu = cpuNanos.clone();
        long[] wall = wallNanos.clone();
        Arrays.sort(cpu);
        Arrays.sort(wall);

        int paintedPercent = -1;
        if (viewportPixels > 0 && Arrays.stream(paintedPixels).anyMatch(value -> value > 0)) {
            long[] painted = paintedPixels.clone();
            Arrays.sort(painted);
            paintedPercent = (int) Math.round(painted[painted.length / 2] * 100.0 / viewportPixels);
        }

        return new EditorBenchmarkResult(
            scenario,
            cpu.length,
            millis(cpu[cpu.length / 2]),
            millis(cpu[Math.min(cpu.length - 1, (int) Math.ceil(cpu.length * 0.9) - 1)]),
            millis(cpu[cpu.length - 1]),
            millis(wall[wall.length / 2]),
            paintedPercent
        );
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
