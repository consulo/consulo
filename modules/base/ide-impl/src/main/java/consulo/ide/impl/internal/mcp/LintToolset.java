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

import consulo.application.progress.EmptyProgressIndicator;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.codeInspection.InspectionEngine;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class LintToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 100;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("lint_files")
            .description("Runs the project's enabled code inspections over the given files and reports what they find, as "
                + "path:line message. Unlike get_file_problems this runs the inspections on demand, so it works on files "
                + "that were never opened in the editor, but it is correspondingly slower.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.stringArray("paths", "Project-relative paths of the files to inspect."))
            .param(McpParameter.integer("limit", "Maximum number of problems returned.").defaultValue(DEFAULT_LIMIT))
            .handler(LintToolset::lintFiles);
    }

    private static CompletableFuture<McpToolCallResult> lintFiles(McpToolCallContext context) {
        Project project = context.getProject();
        List<String> paths = context.getStringList("paths");
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            if (paths.isEmpty()) {
                return McpToolCallResult.error("No paths given.");
            }

            InspectionManager inspectionManager = InspectionManager.getInstance(project);
            InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();

            StringBuilder builder = new StringBuilder();
            int found = 0;

            for (String path : paths) {
                VirtualFile file = McpProjectPaths.resolve(project, path);
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile == null) {
                    builder.append(path).append(": not backed by a PSI tree, skipped\n");
                    continue;
                }

                List<ProblemDescriptor> problems = InspectionEngine.inspect(enabledLocalTools(profile, psiFile),
                                                                            psiFile,
                                                                            inspectionManager,
                                                                            false,
                                                                            false,
                                                                            new EmptyProgressIndicator());

                Document document = FileDocumentManager.getInstance().getDocument(file);
                for (ProblemDescriptor problem : problems) {
                    if (found >= limit) {
                        builder.append("... truncated at ").append(limit).append(" problems\n");
                        return McpToolCallResult.text(builder.toString());
                    }
                    builder.append(path)
                        .append(':')
                        .append(lineOf(document, problem))
                        .append(' ')
                        .append(problem.getDescriptionTemplate())
                        .append('\n');
                    found++;
                }
            }

            if (found == 0) {
                return McpToolCallResult.text("No problems found.");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    /**
     * Only local tools can run per file; global inspections need a whole-project context.
     */
    private static List<LocalInspectionToolWrapper> enabledLocalTools(InspectionProfile profile, PsiFile file) {
        List<LocalInspectionToolWrapper> tools = new ArrayList<>();
        for (InspectionToolWrapper<?> wrapper : profile.getInspectionTools(file)) {
            if (wrapper instanceof LocalInspectionToolWrapper local && profile.isToolEnabled(local.getHighlightDisplayKey(), file)) {
                tools.add(local);
            }
        }
        return tools;
    }

    private static int lineOf(Document document, ProblemDescriptor problem) {
        if (document == null) {
            return problem.getLineNumber() + 1;
        }
        int offset = problem.getPsiElement() == null ? -1 : problem.getPsiElement().getTextRange().getStartOffset();
        if (offset < 0 || offset > document.getTextLength()) {
            return problem.getLineNumber() + 1;
        }
        return document.getLineNumber(offset) + 1;
    }
}
