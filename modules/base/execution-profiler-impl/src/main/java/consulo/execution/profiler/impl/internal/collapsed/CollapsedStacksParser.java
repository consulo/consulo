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

import consulo.execution.profiler.DummyCallTreeBuilder;
import consulo.execution.profiler.LineByLineParser;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.NoThreadInfoInProfilerData;
import consulo.execution.profiler.model.ThreadInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class CollapsedStacksParser extends LineByLineParser {
    private final Function<String, BaseCallStackElement> myFrameFactory;
    private final DummyCallTreeBuilder<BaseCallStackElement> myBuilder = new DummyCallTreeBuilder<>();
    private final Map<String, BaseCallStackElement> myFrames = new HashMap<>();
    private final Map<String, ThreadInfo> myThreads = new HashMap<>();
    private int myStackCount;

    public CollapsedStacksParser() {
        this(CollapsedStacksFormat::parseFrame);
    }

    public CollapsedStacksParser(Function<String, BaseCallStackElement> frameFactory) {
        myFrameFactory = frameFactory;
    }

    public DummyCallTreeBuilder<BaseCallStackElement> getBuilder() {
        return myBuilder;
    }

    public int getStackCount() {
        return myStackCount;
    }

    @Override
    public void consumeLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
            return;
        }

        int valueStart = trimmed.lastIndexOf(' ');
        if (valueStart <= 0) {
            setBadLines(getBadLines() + 1);
            return;
        }

        long value;
        try {
            value = Long.parseLong(trimmed.substring(valueStart + 1));
        }
        catch (NumberFormatException e) {
            setBadLines(getBadLines() + 1);
            return;
        }

        if (value < 0) {
            setBadLines(getBadLines() + 1);
            return;
        }

        ThreadInfo thread = NoThreadInfoInProfilerData.INSTANCE;
        List<BaseCallStackElement> frames = new ArrayList<>();
        String stack = trimmed.substring(0, valueStart);
        int frameStart = 0;
        while (frameStart <= stack.length()) {
            int frameEnd = stack.indexOf(';', frameStart);
            if (frameEnd < 0) {
                frameEnd = stack.length();
            }

            String frame = stack.substring(frameStart, frameEnd).trim();
            if (!frame.isEmpty()) {
                ThreadInfo frameThread = frames.isEmpty() && thread == NoThreadInfoInProfilerData.INSTANCE ? getThread(frame) : null;
                if (frameThread != null) {
                    thread = frameThread;
                }
                else {
                    frames.add(myFrames.computeIfAbsent(frame, myFrameFactory));
                }
            }
            frameStart = frameEnd + 1;
        }

        if (frames.isEmpty()) {
            setBadLines(getBadLines() + 1);
            return;
        }

        myBuilder.addStack(thread, frames, value);
        myStackCount++;
    }

    private @Nullable ThreadInfo getThread(String frame) {
        ThreadInfo thread = myThreads.get(frame);
        if (thread == null) {
            thread = CollapsedStacksFormat.parseThread(frame);
            if (thread != null) {
                myThreads.put(frame, thread);
            }
        }
        return thread;
    }
}
