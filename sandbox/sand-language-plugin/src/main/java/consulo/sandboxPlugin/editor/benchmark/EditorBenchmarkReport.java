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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class EditorBenchmarkReport {
    private final Map<String, String> myEnvironment = new LinkedHashMap<>();
    private final Map<String, Double> mySetup = new LinkedHashMap<>();
    private final List<EditorBenchmarkResult> myResults = new ArrayList<>();
    private final List<String> myErrors = new ArrayList<>();

    public void environment(String key, Object value) {
        myEnvironment.put(key, String.valueOf(value));
    }

    public void setup(String step, long nanos) {
        mySetup.put(step, nanos / 1_000_000.0);
    }

    public void result(EditorBenchmarkResult result) {
        myResults.add(result);
    }

    public void error(String message) {
        myErrors.add(message);
    }

    public void clearMeasurements() {
        myResults.clear();
        myErrors.clear();
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Editor benchmark\n\n");

        int keyWidth = myEnvironment.keySet().stream().mapToInt(String::length).max().orElse(0);
        for (Map.Entry<String, String> entry : myEnvironment.entrySet()) {
            builder.append(pad(entry.getKey(), keyWidth)).append("  ").append(entry.getValue()).append('\n');
        }

        builder.append("\nSetup\n");
        int setupWidth = mySetup.keySet().stream().mapToInt(String::length).max().orElse(0);
        for (Map.Entry<String, Double> entry : mySetup.entrySet()) {
            builder.append("  ").append(pad(entry.getKey(), setupWidth)).append(String.format(Locale.ROOT, "%10.1f ms", entry.getValue()))
                .append('\n');
        }

        builder.append("\nScenarios (CPU time of the UI thread; wall includes waiting for the paint)\n");
        int nameWidth = Math.max(8, myResults.stream().mapToInt(result -> result.scenario().length()).max().orElse(0));
        builder.append("  ").append(pad("scenario", nameWidth))
            .append("  runs   cpu med   cpu p90   cpu max  wall med  painted\n");
        for (EditorBenchmarkResult result : myResults) {
            builder.append("  ").append(pad(result.scenario(), nameWidth))
                .append(String.format(Locale.ROOT, "  %4d %9.2f %9.2f %9.2f %9.2f",
                    result.runs(), result.cpuMedianMs(), result.cpuP90Ms(), result.cpuMaxMs(), result.wallMedianMs()))
                .append(result.paintedPercent() < 0 ? "        -" : String.format(Locale.ROOT, "  %6d%%", result.paintedPercent()))
                .append('\n');
        }

        if (!myErrors.isEmpty()) {
            builder.append("\nErrors\n");
            for (String error : myErrors) {
                builder.append("  ").append(error).append('\n');
            }
        }
        return builder.toString();
    }

    private static String pad(String value, int width) {
        return value.length() >= width ? value : value + " ".repeat(width - value.length());
    }
}
