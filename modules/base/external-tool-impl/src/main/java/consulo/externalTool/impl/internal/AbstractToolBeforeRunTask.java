/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalTool.impl.internal;

import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.ui.UIAccess;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Transient;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author traff
 */
public abstract class AbstractToolBeforeRunTask<T extends Tool> extends BeforeRunTask {
    private @Nullable String myToolActionId;

    @Attribute("actionId")
    public @Nullable String getToolActionId() {
        return myToolActionId;
    }

    public void setToolActionId(@Nullable String toolActionId) {
        myToolActionId = toolActionId;
    }

    @Transient
    public boolean isExecutable() {
        return myToolActionId != null;
    }

    public CompletableFuture<Void> execute(UIAccess uiAccess, DataContext context, long executionId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        uiAccess.giveAsync(() -> {
            boolean runToolResult = ToolAction.runTool(
                myToolActionId,
                context,
                null,
                executionId,
                new ProcessAdapter() {
                    @Override
                    public void processTerminated(ProcessEvent event) {
                        if (event.getExitCode() == 0) {
                            result.complete(null);
                        }
                        else {
                            result.completeExceptionally(new CancellationException());
                        }
                    }
                }
            );

            if (!runToolResult) {
                result.completeExceptionally(new CancellationException());
            }
        }).exceptionally(error -> {
            result.completeExceptionally(error);
            return null;
        });

        return result;
    }

    public @Nullable T findCorrespondingTool() {
        if (myToolActionId == null) {
            return null;
        }
        for (T tool : availableTools()) {
            if (myToolActionId.equals(tool.getActionId())) {
                return tool;
            }
        }
        return null;
    }

    protected abstract List<T> availableTools();
}
