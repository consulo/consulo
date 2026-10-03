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
package consulo.execution.profiler.impl.internal.collapsed;

import consulo.execution.profiler.CallTreeBuilder;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.CollapsedProfilerDumpWriter;
import consulo.execution.profiler.ProfilerDumpWriter;
import consulo.execution.profiler.model.NativeCall;
import consulo.execution.profiler.model.NativeThread;
import consulo.execution.profiler.model.ThreadInfo;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CollapsedStacksFormat {
    public static final String COLLAPSED_EXTENSION = "collapsed";
    public static final String FOLDED_EXTENSION = "folded";

    private static final String THREAD_ID_MARKER = " tid=";

    private CollapsedStacksFormat() {
    }

    public static ProfilerDumpWriter createWriter(
        CallTreeBuilder<BaseCallStackElement> builder,
        String processName,
        long attachedTimestamp
    ) {
        return new CollapsedProfilerDumpWriter(
            builder,
            processName,
            attachedTimestamp,
            BaseCallStackElement::fullName,
            CollapsedStacksFormat::formatThread
        );
    }

    public static String formatThread(ThreadInfo thread) {
        long id = thread instanceof NativeThread nativeThread ? nativeThread.getId() : 0;
        return "[" + thread.getName() + THREAD_ID_MARKER + id + "]";
    }

    public static @Nullable ThreadInfo parseThread(String frame) {
        if (frame.length() < 2 || frame.charAt(0) != '[' || frame.charAt(frame.length() - 1) != ']') {
            return null;
        }

        String content = frame.substring(1, frame.length() - 1);
        int marker = content.lastIndexOf(THREAD_ID_MARKER);
        if (marker < 0) {
            return null;
        }

        try {
            long id = Long.parseLong(content.substring(marker + THREAD_ID_MARKER.length()).trim());
            return new NativeThread(id, content.substring(0, marker));
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static BaseCallStackElement parseFrame(String frame) {
        if (frame.indexOf('`') >= 0) {
            NativeCall call = NativeCall.read(frame);
            if (call != null) {
                return call;
            }
        }
        return new CollapsedCallStackElement(frame);
    }
}
