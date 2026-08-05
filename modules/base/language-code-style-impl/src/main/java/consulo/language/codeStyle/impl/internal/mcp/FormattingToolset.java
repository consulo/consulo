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
package consulo.language.codeStyle.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class FormattingToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("reformat_file")
            .description("Reformats a project file with the code style settings of the project, exactly as Reformat Code does. Modifies "
                + "the file in place; the change lands in the editor and can be undone by the user.")
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file to reformat."))
            .handler(FormattingToolset::reformatFile);
    }

    private static CompletableFuture<McpToolCallResult> reformatFile(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");

        return McpToolActions.writeAction(project, () -> {
            VirtualFile file = McpProjectPaths.resolve(project, path);
            if (file.isDirectory()) {
                return McpToolCallResult.error("Not a file: " + path);
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return McpToolCallResult.error("File is not backed by a PSI tree: " + path);
            }

            CodeStyleManager.getInstance(project).reformat(psiFile);
            return McpToolCallResult.success();
        });
    }
}
