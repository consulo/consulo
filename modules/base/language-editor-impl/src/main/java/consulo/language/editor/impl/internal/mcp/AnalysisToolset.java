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
package consulo.language.editor.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.annotation.HighlightSeverity;
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
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class AnalysisToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 100;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_file_problems")
            .description("Lists the errors and warnings the IDE has highlighted in a file, as path:line:column severity: "
                + "message. Reads the highlights the code analyzer has already produced, so a file that has never been "
                + "opened and analyzed reports nothing - open_file_in_editor it first. Use build_project for a compiler "
                + "answer that does not depend on what the editor has looked at.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file to inspect."))
            .param(McpParameter.bool("errorsOnly", "Whether to report only errors and skip warnings.").defaultValue(false))
            .param(McpParameter.integer("limit", "Maximum number of problems returned.").defaultValue(DEFAULT_LIMIT))
            .handler(AnalysisToolset::getFileProblems);
    }

    private static CompletableFuture<McpToolCallResult> getFileProblems(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        boolean errorsOnly = context.getBoolean("errorsOnly");
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            VirtualFile file = McpProjectPaths.resolve(project, path);

            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return McpToolCallResult.error("File has no document: " + path);
            }

            HighlightSeverity minSeverity = errorsOnly ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;

            StringBuilder builder = new StringBuilder();
            int[] found = {0};

            DaemonCodeAnalyzer.processHighlights(document, project, minSeverity, 0, document.getTextLength(), info -> {
                builder.append(path)
                    .append(':')
                    .append(lineOf(document, info))
                    .append(':')
                    .append(columnOf(document, info))
                    .append(' ')
                    .append(info.getSeverity().getName())
                    .append(": ")
                    .append(info.getDescription().get())
                    .append('\n');
                return ++found[0] < limit;
            });

            if (found[0] == 0) {
                return McpToolCallResult.text("No problems reported for " + path
                    + ". Note that a file the analyzer has not looked at yet always answers this way.");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static int lineOf(Document document, HighlightInfo info) {
        return document.getLineNumber(clamp(document, info.getActualStartOffset())) + 1;
    }

    private static int columnOf(Document document, HighlightInfo info) {
        int offset = clamp(document, info.getActualStartOffset());
        return offset - document.getLineStartOffset(document.getLineNumber(offset)) + 1;
    }

    private static int clamp(Document document, int offset) {
        return Math.max(0, Math.min(offset, document.getTextLength()));
    }
}
