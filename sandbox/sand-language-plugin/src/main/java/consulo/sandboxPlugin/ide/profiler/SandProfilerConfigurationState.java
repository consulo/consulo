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
package consulo.sandboxPlugin.ide.profiler;

import consulo.execution.profiler.configuration.ProfilerConfigurationStateBase;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandProfilerConfigurationState extends ProfilerConfigurationStateBase {
    public static final int DEFAULT_SAMPLING_INTERVAL_MS = 10;
    public static final int MIN_SAMPLING_INTERVAL_MS = 1;
    public static final int MAX_SAMPLING_INTERVAL_MS = 1000;

    public static final int DEFAULT_SYNTHETIC_THREAD_COUNT = 4;
    public static final int MIN_SYNTHETIC_THREAD_COUNT = 1;
    public static final int MAX_SYNTHETIC_THREAD_COUNT = 32;

    private int mySamplingIntervalMs = DEFAULT_SAMPLING_INTERVAL_MS;
    private int mySyntheticThreadCount = DEFAULT_SYNTHETIC_THREAD_COUNT;

    @Override
    public String getConfigurationTypeId() {
        return SandProfilerConfigurationType.ID;
    }

    public int getSamplingIntervalMs() {
        return mySamplingIntervalMs;
    }

    public void setSamplingIntervalMs(int samplingIntervalMs) {
        mySamplingIntervalMs = Math.clamp(samplingIntervalMs, MIN_SAMPLING_INTERVAL_MS, MAX_SAMPLING_INTERVAL_MS);
    }

    public int getSyntheticThreadCount() {
        return mySyntheticThreadCount;
    }

    public void setSyntheticThreadCount(int syntheticThreadCount) {
        mySyntheticThreadCount = Math.clamp(syntheticThreadCount, MIN_SYNTHETIC_THREAD_COUNT, MAX_SYNTHETIC_THREAD_COUNT);
    }
}
