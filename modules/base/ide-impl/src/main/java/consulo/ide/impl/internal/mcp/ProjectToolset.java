/*
 * Copyright 2000-2026 JetBrains s.r.o.
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
package consulo.ide.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class ProjectToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_project_modules")
            .description("Lists the names of all modules of the currently open project.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(ProjectToolset::getProjectModules);
    }

    private static CompletableFuture<McpToolCallResult> getProjectModules(McpToolCallContext context) {
        CoroutineScope scope = CoroutineScope.of(Application.get().coroutineContext());
        return Coroutine.first(ReadLock.<Object, McpToolCallResult>apply(ignored -> {
            StringBuilder builder = new StringBuilder();
            for (Module module : ModuleManager.getInstance(context.getProject()).getModules()) {
                builder.append(module.getName()).append('\n');
            }
            return McpToolCallResult.text(builder.toString());
        })).runAsync(scope, null).toFuture();
    }
}
