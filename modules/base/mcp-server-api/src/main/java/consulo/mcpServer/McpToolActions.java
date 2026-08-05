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
package consulo.mcpServer;

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.mcp.tool.McpToolCallResult;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runs a tool body in the context it needs. A tool handler is called on the transport thread, which
 * holds no read lock and is not the UI thread, so the model must only be touched through these.
 * <p>
 * Everything runs in the project's coroutine context, which already carries the {@code UIAccess} the
 * UI step needs - the application context does not, so an application-wide scope would fail there.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolActions {
    /** The first step takes no input, but the coroutine entry point still wants a value. */
    private static final Object NO_INPUT = new Object();

    public static CompletableFuture<McpToolCallResult> readAction(Project project, Supplier<McpToolCallResult> supplier) {
        return run(project, ReadLock.<Object, McpToolCallResult>apply(ignored -> supplier.get()));
    }

    public static CompletableFuture<McpToolCallResult> writeAction(Project project, Supplier<McpToolCallResult> supplier) {
        return run(project, WriteLock.<Object, McpToolCallResult>apply(ignored -> supplier.get()));
    }

    public static CompletableFuture<McpToolCallResult> uiAction(Project project, Supplier<McpToolCallResult> supplier) {
        return run(project, UIAction.<Object, McpToolCallResult>apply(ignored -> supplier.get()));
    }

    private static CompletableFuture<McpToolCallResult> run(Project project, CoroutineStep<Object, McpToolCallResult> step) {
        CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
        return Coroutine.first(step).runAsync(scope, NO_INPUT).toFuture();
    }

    private McpToolActions() {
    }
}
