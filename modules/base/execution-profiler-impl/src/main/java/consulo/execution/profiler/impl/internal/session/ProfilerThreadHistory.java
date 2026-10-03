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

import consulo.execution.profiler.live.ProfilerMonitorSink;
import consulo.execution.profiler.live.ProfilerThreadState;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class ProfilerThreadHistory {
    private static final int MAX_CHANGES = 10_000;
    private static final int INITIAL_CAPACITY = 8;

    private final long myThreadId;

    private String myName;
    private long[] myTimes = new long[INITIAL_CAPACITY];
    private ProfilerThreadState[] myStates = new ProfilerThreadState[INITIAL_CAPACITY];
    private int mySize;

    ProfilerThreadHistory(long threadId, String name) {
        myThreadId = threadId;
        myName = name;
    }

    long getThreadId() {
        return myThreadId;
    }

    long getFirstTime() {
        return myTimes[0];
    }

    long getLastTime() {
        return myTimes[mySize - 1];
    }

    boolean isTerminated() {
        return mySize > 0 && myStates[mySize - 1] == ProfilerThreadState.TERMINATED;
    }

    boolean isTerminatedBefore(long timeMicros) {
        return isTerminated() && getLastTime() < timeMicros;
    }

    boolean set(String name, long timeMicros, ProfilerThreadState state) {
        myName = name;
        if (mySize > 0 && (myStates[mySize - 1] == state || myTimes[mySize - 1] > timeMicros)) {
            return false;
        }

        if (mySize == myTimes.length) {
            if (mySize >= MAX_CHANGES * 2) {
                int dropped = mySize - MAX_CHANGES;
                System.arraycopy(myTimes, dropped, myTimes, 0, MAX_CHANGES);
                System.arraycopy(myStates, dropped, myStates, 0, MAX_CHANGES);
                Arrays.fill(myStates, MAX_CHANGES, myStates.length, null);
                mySize = MAX_CHANGES;
            }
            else {
                int capacity = Math.min(myTimes.length * 2, MAX_CHANGES * 2);
                myTimes = Arrays.copyOf(myTimes, capacity);
                myStates = Arrays.copyOf(myStates, capacity);
            }
        }

        myTimes[mySize] = timeMicros;
        myStates[mySize] = state;
        mySize++;
        return true;
    }

    void replay(ProfilerMonitorSink sink, long fromMicros) {
        for (int i = findReplayStart(fromMicros); i < mySize; i++) {
            long time = Math.max(myTimes[i], fromMicros);
            sink.threadState(myThreadId, myName, ProfilerLiveHistory.toInstant(time), myStates[i]);
        }
    }

    private int findReplayStart(long fromMicros) {
        int low = 0;
        int high = mySize - 1;
        int start = 0;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (myTimes[middle] <= fromMicros) {
                start = middle;
                low = middle + 1;
            }
            else {
                high = middle - 1;
            }
        }
        return start;
    }
}
