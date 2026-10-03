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
package consulo.execution.profiler.impl.internal.session;

import consulo.execution.profiler.live.ProfilerMetric;
import consulo.execution.profiler.live.ProfilerMonitorSink;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class ProfilerMetricHistory {
    private static final int MAX_SAMPLES = 86_400;
    private static final int INITIAL_CAPACITY = 64;

    private final ProfilerMetric myMetric;

    private long[] myTimes = new long[INITIAL_CAPACITY];
    private double[] myValues = new double[INITIAL_CAPACITY];
    private int mySize;

    ProfilerMetricHistory(ProfilerMetric metric) {
        myMetric = metric;
    }

    ProfilerMetric getMetric() {
        return myMetric;
    }

    long getFirstTime() {
        return myTimes[0];
    }

    long getLastTime() {
        return myTimes[mySize - 1];
    }

    boolean add(long timeMicros, double value) {
        boolean dropped = false;
        if (mySize == myTimes.length) {
            if (mySize >= MAX_SAMPLES * 2) {
                int droppedCount = mySize - MAX_SAMPLES;
                System.arraycopy(myTimes, droppedCount, myTimes, 0, MAX_SAMPLES);
                System.arraycopy(myValues, droppedCount, myValues, 0, MAX_SAMPLES);
                mySize = MAX_SAMPLES;
                dropped = true;
            }
            else {
                int capacity = Math.min(myTimes.length * 2, MAX_SAMPLES * 2);
                myTimes = Arrays.copyOf(myTimes, capacity);
                myValues = Arrays.copyOf(myValues, capacity);
            }
        }

        myTimes[mySize] = timeMicros;
        myValues[mySize] = value;
        mySize++;
        return dropped;
    }

    void replay(ProfilerMonitorSink sink) {
        for (int i = 0; i < mySize; i++) {
            sink.metric(myMetric, ProfilerLiveHistory.toInstant(myTimes[i]), myValues[i]);
        }
    }
}
