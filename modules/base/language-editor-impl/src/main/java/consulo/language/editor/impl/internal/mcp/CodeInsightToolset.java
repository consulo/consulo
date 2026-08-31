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
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
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
public class CodeInsightToolset implements McpToolset {
    private static final int MAX_DECLARATION_LENGTH = 2000;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_symbol_info")
            .description("Describes the symbol at the given 1-based line and column. If the position sits on a usage, the reference is "
                + "resolved first, so the answer describes the declaration rather than the usage. Reports name, element type, language, "
                + "declaring file and the declaration text.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file to inspect."))
            .param(McpParameter.integer("line", "1-based line number."))
            .param(McpParameter.integer("column", "1-based column number."))
            .handler(CodeInsightToolset::getSymbolInfo);
    }

    private static CompletableFuture<McpToolCallResult> getSymbolInfo(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int line = context.getInt("line");
        int column = context.getInt("column");

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

            return McpToolCallResult.text(describe(project, target));
        });
    }

    /**
     * A position on a usage should describe the declaration it points at, not the usage itself.
     */
    private static PsiElement resolve(PsiFile psiFile, int offset) {
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

    private static String describe(Project project, PsiElement target) {
        StringBuilder builder = new StringBuilder();

        if (target instanceof PsiNamedElement named) {
            builder.append("name: ").append(named.getName()).append('\n');
        }
        builder.append("element: ").append(target.getClass().getSimpleName()).append('\n');
        builder.append("language: ").append(target.getLanguage().getDisplayName()).append('\n');

        PsiFile containingFile = target.getContainingFile();
        VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
        if (virtualFile != null) {
            builder.append("file: ").append(McpProjectPaths.relativePath(project, virtualFile)).append('\n');
        }

        String text = target.getText();
        if (text != null) {
            builder.append("declaration:\n")
                .append(text.length() > MAX_DECLARATION_LENGTH ? text.substring(0, MAX_DECLARATION_LENGTH) + "..." : text);
        }
        return builder.toString();
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
