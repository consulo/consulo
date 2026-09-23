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
package consulo.ide.impl.idea.build;

import consulo.build.ui.event.BuildEventPresentationData;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.Failure;
import consulo.disposer.Disposable;
import consulo.execution.ui.ExecutionConsole;
import org.jspecify.annotations.Nullable;

/**
 * The consoles of a build, one per node of its tree - what a node printed is its own, and selecting the node
 * is what puts it on screen. What draws them belongs to the frontend the view was built for.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public interface BuildNodeConsoleHandler extends Disposable {
    void addOutput(ExecutionNodeImpl node, Object buildId, BuildEvent event);

    void addOutput(ExecutionNodeImpl node, String text, boolean stdOut);

    void addOutput(ExecutionNodeImpl node, Failure failure);

    boolean setNode(@Nullable ExecutionNodeImpl node);

    /**
     * Whether a node was ever selected. A build which ends with nothing selected shows the build itself.
     */
    boolean hasNode();

    void maybeAddExecutionConsole(ExecutionNodeImpl node, BuildEventPresentationData presentationData);

    void updateProgressBar(long total, long progress);

    void stopProgressBar();

    void clear();

    @Nullable ExecutionConsole getCurrentConsole();
}
