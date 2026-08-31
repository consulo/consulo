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
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.navigation.NavigationItem;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class SymbolSearchToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 50;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("search_symbol")
            .description("Finds declared symbols - classes, methods, fields - whose name contains the given substring. Uses the IDE's Go "
                + "to Symbol index, so it only covers languages with symbol support and only project sources, not libraries.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("nameSubstring", "Substring to look for in symbol names."))
            .param(McpParameter.integer("limit", "Maximum number of results.").defaultValue(DEFAULT_LIMIT))
            .handler(SymbolSearchToolset::searchSymbol);
    }

    private static CompletableFuture<McpToolCallResult> searchSymbol(McpToolCallContext context) {
        Project project = context.getProject();
        String nameSubstring = context.getString("nameSubstring").toLowerCase(Locale.ROOT);
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            int[] found = {0};

            Application.get().getExtensionPoint(GotoSymbolContributor.class).forEach(contributor -> {
                if (found[0] >= limit) {
                    return;
                }

                for (String name : contributor.getNames(project, false)) {
                    if (found[0] >= limit) {
                        return;
                    }
                    if (!name.toLowerCase(Locale.ROOT).contains(nameSubstring)) {
                        continue;
                    }

                    for (NavigationItem item : contributor.getItemsByName(name, name, project, false)) {
                        builder.append(describe(project, item)).append('\n');
                        if (++found[0] >= limit) {
                            return;
                        }
                    }
                }
            });

            return McpToolCallResult.text(builder.toString());
        });
    }

    private static String describe(Project project, NavigationItem item) {
        StringBuilder builder = new StringBuilder(String.valueOf(item.getName()));

        ItemPresentation presentation = item.getPresentation();
        if (presentation != null) {
            String location = presentation.getLocationString();
            if (location != null && !location.isEmpty()) {
                builder.append(" (").append(location).append(')');
            }
        }

        VirtualFile file = item instanceof consulo.language.psi.PsiElement element && element.getContainingFile() != null
            ? element.getContainingFile().getVirtualFile()
            : null;
        if (file != null) {
            builder.append(" - ").append(McpProjectPaths.relativePath(project, file));
        }
        return builder.toString();
    }
}
