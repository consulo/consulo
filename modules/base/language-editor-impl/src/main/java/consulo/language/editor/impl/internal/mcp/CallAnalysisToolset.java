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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
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
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class CallAnalysisToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 100;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("analyze_calls")
            .description("Finds everywhere the symbol at the given position is referenced, reporting each usage as "
                + "path:line:column with its source line. This is a flat reference search over project sources, one level "
                + "deep - it does not build a recursive call hierarchy and does not search libraries.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file holding the symbol."))
            .param(McpParameter.integer("line", "1-based line number of the symbol."))
            .param(McpParameter.integer("column", "1-based column number of the symbol."))
            .param(McpParameter.integer("limit", "Maximum number of usages returned.").defaultValue(DEFAULT_LIMIT))
            .handler(CallAnalysisToolset::analyzeCalls);
    }

    private static CompletableFuture<McpToolCallResult> analyzeCalls(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int line = context.getInt("line");
        int column = context.getInt("column");
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            VirtualFile file = McpProjectPaths.resolve(project, path);

            Document document = FileDocumentManager.getInstance().getDocument(file);
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (document == null || psiFile == null) {
                return McpToolCallResult.error("File is not backed by a PSI tree: " + path);
            }

            int offset = offsetOf(document, line, column);
            if (offset < 0) {
                return McpToolCallResult.error("Position is outside the file: " + line + ":" + column);
            }

            PsiElement target = resolve(psiFile, offset);
            if (target == null) {
                return McpToolCallResult.error("No symbol found at " + path + ":" + line + ":" + column);
            }

            StringBuilder builder = new StringBuilder();
            int[] found = {0};

            ReferencesSearch.search(target).forEach(reference -> {
                String usage = describe(project, reference);
                if (usage != null) {
                    builder.append(usage).append('\n');
                }
                return ++found[0] < limit;
            });

            if (found[0] == 0) {
                return McpToolCallResult.text("No usages found.");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static @Nullable String describe(Project project, PsiReference reference) {
        PsiElement element = reference.getElement();
        PsiFile containingFile = element.getContainingFile();
        VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return McpProjectPaths.relativePath(project, virtualFile);
        }

        int offset = Math.min(element.getTextRange().getStartOffset(), document.getTextLength());
        int lineNumber = document.getLineNumber(offset);
        int lineStart = document.getLineStartOffset(lineNumber);

        return McpProjectPaths.relativePath(project, virtualFile)
            + ':' + (lineNumber + 1)
            + ':' + (offset - lineStart + 1)
            + ": " + document.getText().substring(lineStart, document.getLineEndOffset(lineNumber)).strip();
    }

    private static @Nullable PsiElement resolve(PsiFile psiFile, int offset) {
        PsiReference reference = psiFile.findReferenceAt(offset);
        if (reference != null) {
            PsiElement resolved = reference.resolve();
            if (resolved != null) {
                return resolved;
            }
        }

        PsiElement element = psiFile.findElementAt(offset);
        return element == null ? null : element.getParent();
    }

    private static int offsetOf(Document document, int line, int column) {
        int lineIndex = line - 1;
        if (lineIndex < 0 || lineIndex >= document.getLineCount()) {
            return -1;
        }

        int lineStart = document.getLineStartOffset(lineIndex);
        int offset = lineStart + Math.max(0, column - 1);
        return offset > document.getLineEndOffset(lineIndex) ? -1 : offset;
    }
}
