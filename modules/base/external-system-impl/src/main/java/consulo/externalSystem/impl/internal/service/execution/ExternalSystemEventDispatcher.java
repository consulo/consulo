/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalSystem.impl.internal.service.execution;

import consulo.application.Application;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.output.BuildOutputInstantReader;
import consulo.build.ui.output.BuildOutputParser;
import consulo.build.ui.output.BuildOutputService;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.service.execution.ExternalSystemOutputParserProvider;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Routes external system task output to a {@link BuildProgressListener} (typically
 * {@link consulo.build.ui.SyncViewManager}).
 * <p>
 * Text appended via {@link #append} is piped through a chain of
 * {@link BuildOutputParser}s collected from all registered
 * {@link ExternalSystemOutputParserProvider} EPs that match the task's
 * {@link ExternalSystemTaskId#getProjectSystemId() project system id}.  Parsers emit structured
 * {@link BuildEvent}s (file-level errors with clickable file:line links, warnings, etc.).
 * <p>
 * When no parsers are registered the append methods are no-ops and text is only forwarded via
 * explicit {@link #onEvent} calls.
 * <p>
 * Must be {@link #close}d when the task ends to flush any remaining buffered text through the
 * parser chain before the final {@code FinishBuildEvent} is fired.
 */
public class ExternalSystemEventDispatcher implements BuildProgressListener, Appendable, Closeable {

    private final BuildProgressListener myDelegate;

    /**
     * Non-null when at least one {@link ExternalSystemOutputParserProvider} provides parsers for the task.
     */
    private final BuildOutputInstantReader.@Nullable Primary myOutputReader;

    /**
     * @param taskId   the task whose output will be dispatched
     * @param delegate the listener that will receive all {@link BuildEvent}s (e.g. {@code SyncViewManager})
     */
    public ExternalSystemEventDispatcher(ExternalSystemTaskId taskId, BuildProgressListener delegate) {
        myDelegate = delegate;

        List<BuildOutputParser> parsers = new ArrayList<>();
        ExternalSystemOutputParserProvider.EP_NAME.getExtensionList().forEach(provider -> {
            if (taskId.getProjectSystemId().equals(provider.getExternalSystemId())) {
                parsers.addAll(provider.getBuildOutputParsers(taskId));
            }
        });

        if (!parsers.isEmpty()) {
            myOutputReader = Application.get().getInstance(BuildOutputService.class)
                .createBuildOutputInstantReader(taskId, taskId, delegate, parsers);
        }
        else {
            myOutputReader = null;
        }
    }

    // ---- BuildProgressListener ------------------------------------------------------------------

    /** Forwards the event directly to the delegate listener. */
    @Override
    public void onEvent(Object buildId, BuildEvent event) {
        myDelegate.onEvent(buildId, event);
    }

    // ---- Appendable (parser chain) --------------------------------------------------------------

    /**
     * Appends {@code csq} to the parser's read buffer.
     * Has no effect when no parsers were registered.
     */
    @Override
    public Appendable append(CharSequence csq) throws IOException {
        if (myOutputReader != null) {
            myOutputReader.append(csq);
        }
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        if (myOutputReader != null) {
            myOutputReader.append(csq, start, end);
        }
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        if (myOutputReader != null) {
            myOutputReader.append(c);
        }
        return this;
    }

    // ---- Closeable ------------------------------------------------------------------------------

    /**
     * Closes the underlying {@link BuildOutputInstantReader}, flushing any remaining buffered
     * text through the parser chain before returning.
     * <p>
     * Call this <em>before</em> firing the final {@code FinishBuildEvent} so that all parser-
     * generated file-level messages appear in the tree before the build is marked as done.
     */
    @Override
    public void close() throws IOException {
        if (myOutputReader != null) {
            myOutputReader.close();
        }
    }
}
