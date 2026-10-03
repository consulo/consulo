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
package consulo.execution.profiler;

import consulo.application.progress.ProgressIndicator;
import consulo.execution.profiler.model.NoThreadInfoInProfilerData;
import consulo.execution.profiler.model.ThreadInfo;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Writes call stacks in the collapsed-stacks format: one {@code thread;root;...;leaf value} line per stack.
 * The thread is left out for data without thread information.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class CollapsedProfilerDumpWriter implements ProfilerDumpWriter {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final String EXTENSION = ".collapsed";

    private final CallTreeBuilder<BaseCallStackElement> myBuilder;
    private final String myProcessName;
    private final long myAttachedTimestamp;
    private final Function<BaseCallStackElement, String> myFrameName;
    private final Function<ThreadInfo, String> myThreadName;

    public CollapsedProfilerDumpWriter(
        CallTreeBuilder<BaseCallStackElement> builder,
        String processName,
        long attachedTimestamp,
        Function<BaseCallStackElement, String> frameName,
        Function<ThreadInfo, String> threadName
    ) {
        myBuilder = builder;
        myProcessName = processName;
        myAttachedTimestamp = attachedTimestamp;
        myFrameName = frameName;
        myThreadName = threadName;
    }

    @Override
    public String getDumpFileName() {
        StringBuilder name = new StringBuilder(myProcessName.length());
        for (int i = 0; i < myProcessName.length(); i++) {
            char c = myProcessName.charAt(i);
            name.append(Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_' ? c : '_');
        }
        if (name.isEmpty()) {
            name.append("profile");
        }
        return name + "_" + TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(myAttachedTimestamp)) + EXTENSION;
    }

    @Override
    public void writeDump(File file, ProgressIndicator indicator) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder line = new StringBuilder();
            for (Stack<BaseCallStackElement> stack : myBuilder.getAllStacks()) {
                indicator.checkCanceled();

                if (stack.getFrames().isEmpty()) {
                    continue;
                }

                line.setLength(0);
                ThreadInfo thread = stack.getThread();
                if (thread != NoThreadInfoInProfilerData.INSTANCE) {
                    appendName(line, myThreadName.apply(thread));
                }
                for (BaseCallStackElement frame : stack.getFrames()) {
                    if (!line.isEmpty()) {
                        line.append(';');
                    }
                    appendName(line, myFrameName.apply(frame));
                }
                line.append(' ').append(stack.getValue()).append('\n');
                writer.append(line);
            }
        }
    }

    private static void appendName(StringBuilder line, String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            line.append(c == '\n' || c == '\r' ? ' ' : c);
        }
    }
}
