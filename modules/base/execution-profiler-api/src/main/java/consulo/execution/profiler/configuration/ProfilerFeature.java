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
package consulo.execution.profiler.configuration;

/**
 * What a profiler can do with a particular process.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public enum ProfilerFeature {
    /**
     * Live CPU, memory and thread charts.
     */
    LIVE_MONITORING,
    /**
     * Starting and stopping a CPU recording.
     */
    CPU_RECORDING,
    /**
     * Dumping the heap.
     */
    HEAP_DUMP,
    /**
     * Dumping the stacks of all threads.
     */
    THREAD_DUMP
}
