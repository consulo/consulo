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
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.DumbService;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public McpToolGroup getGroup() {
        return McpToolGroup.CODE_ANALYSIS;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_file_problems")
            .description("Analyzes a file now, the way the editor does, and lists its errors and warnings as path:line:column "
                + "severity: message. Works for files that are not open; unsaved editor changes are included. Waits while the IDE "
                + "is indexing.")
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

        return McpToolActions.uiActionAsync(project, () -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            VirtualFile file = McpProjectPaths.resolve(project, path);
            HighlightSeverity minSeverity = errorsOnly ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
            return CompletableFuture.supplyAsync(() -> analyze(project, file, path, minSeverity, limit), AppExecutorUtil.getAppExecutorService());
        });
    }

    private static McpToolCallResult analyze(Project project, VirtualFile file, String path, HighlightSeverity minSeverity, int limit) {
        DaemonProgressIndicator indicator = new DaemonProgressIndicator();
        McpToolCallResult[] result = {McpToolCallResult.error("File is not backed by a PSI tree: " + path)};
        ProgressManager.getInstance().runProcess(() -> DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (psiFile == null || document == null) {
                return;
            }

            DaemonCodeAnalyzerInternal codeAnalyzer = (DaemonCodeAnalyzerInternal) DaemonCodeAnalyzer.getInstance(project);
            List<HighlightInfo> infos = new ArrayList<>();
            for (HighlightInfo info : codeAnalyzer.runMainPasses(psiFile, document, indicator)) {
                if (info.getSeverity().compareTo(minSeverity) >= 0 && !info.getDescription().get().isEmpty()) {
                    infos.add(info);
                }
            }
            infos.sort(Comparator.comparingInt(HighlightInfo::getActualStartOffset));

            StringBuilder builder = new StringBuilder();
            Set<String> seen = new HashSet<>();
            int found = 0;
            for (HighlightInfo info : infos) {
                String line = path + ':' + lineOf(document, info) + ':' + columnOf(document, info) + ' '
                    + info.getSeverity().getName() + ": " + info.getDescription().get();
                if (!seen.add(line)) {
                    continue;
                }
                if (found++ >= limit) {
                    builder.append("... more problems omitted, raise 'limit' to see them\n");
                    break;
                }
                builder.append(line).append('\n');
            }
            result[0] = McpToolCallResult.text(found == 0 ? "No problems found in " + path : builder.toString());
        }), indicator);
        return result[0];
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
