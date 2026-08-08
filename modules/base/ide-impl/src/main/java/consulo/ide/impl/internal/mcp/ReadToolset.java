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
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class ReadToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("read_file")
            .description("Reads the text of a project file by project-relative path. Returns the in-memory editor content when the file is "
                + "open, so unsaved changes are included and the result can differ from what is on disk.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file to read."))
            .handler(ReadToolset::readFile);
    }

    private static CompletableFuture<McpToolCallResult> readFile(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");

        return McpToolActions.readAction(project, () -> {
            VirtualFile file = McpProjectPaths.resolve(project, path);
            if (file.isDirectory()) {
                return McpToolCallResult.error("Not a file: " + path);
            }

            // the in-memory document wins, so an agent sees what the user sees rather than the last saved state
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                return McpToolCallResult.text(document.getText());
            }

            try {
                return McpToolCallResult.text(VirtualFileUtil.loadText(file));
            }
            catch (IOException e) {
                throw new McpToolException("Cannot read " + path + ": " + e.getMessage());
            }
        });
    }
}
