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

import consulo.application.progress.ProgressIndicator;
import consulo.execution.profiler.Failure;
import consulo.execution.profiler.NewCallTreeOnlyProfilerData;
import consulo.execution.profiler.ProfilerDumpFileParser;
import consulo.execution.profiler.ProfilerDumpFileParsingResult;
import consulo.execution.profiler.Success;
import consulo.execution.profiler.ui.NativeCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class CollapsedStacksDumpFileParser implements ProfilerDumpFileParser {
    private static final Logger LOG = Logger.getInstance(CollapsedStacksDumpFileParser.class);

    @Override
    public ProfilerDumpFileParsingResult parse(File fileOrDirectory, ProgressIndicator indicator) {
        if (fileOrDirectory.isDirectory()) {
            return new Failure(LocalizeValue.localizeTODO("A collapsed stacks snapshot is a file, not a directory").get());
        }

        CollapsedStacksParser parser = createParser();
        try (InputStream stream = Files.newInputStream(fileOrDirectory.toPath())) {
            parser.readFromStream(stream, indicator);
        }
        catch (IOException e) {
            LOG.warn("Can't read collapsed stacks from " + fileOrDirectory, e);
            String message = e.getMessage();
            return new Failure(message == null ? LocalizeValue.localizeTODO("Can't read the snapshot file").get() : message);
        }

        if (parser.getStackCount() == 0) {
            if (parser.getBadLines() > 0) {
                return new Failure(
                    LocalizeValue.localizeTODO("No call stacks found: " + parser.getBadLines() + " lines are not collapsed stacks").get()
                );
            }
            return new Failure(LocalizeValue.localizeTODO("The snapshot holds no call stacks").get());
        }

        return new Success(new NewCallTreeOnlyProfilerData(parser.getBuilder(), NativeCallStackElementRenderer.INSTANCE));
    }

    protected CollapsedStacksParser createParser() {
        return new CollapsedStacksParser();
    }
}
