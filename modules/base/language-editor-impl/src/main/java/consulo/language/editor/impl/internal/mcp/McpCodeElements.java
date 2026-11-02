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
package consulo.language.editor.impl.internal.mcp;

import consulo.document.Document;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.module.content.ProjectFileIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.usage.UsageViewTypeLocation;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
final class McpCodeElements {
    static CompletableFuture<McpToolCallResult> committedReadAction(Project project, Supplier<McpToolCallResult> supplier) {
        return McpToolActions.uiActionAsync(project, () -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            if (DumbService.isDumb(project)) {
                return CompletableFuture.completedFuture(McpToolCallResult.error("The IDE is building its indexes, retry when indexing has finished."));
            }
            return McpToolActions.readAction(project, supplier);
        });
    }

    static @Nullable PsiElement findTarget(PsiFile psiFile, int offset) {
        PsiReference reference = psiFile.findReferenceAt(offset);
        if (reference != null) {
            PsiElement resolved = reference.resolve();
            if (resolved != null) {
                return resolved;
            }
        }

        PsiElement leaf = psiFile.findElementAt(offset);
        return leaf == null ? null : findDeclaration(leaf);
    }

    static @Nullable PsiNamedElement findDeclaration(PsiElement leaf) {
        PsiElement parent = leaf.getParent();
        if (parent instanceof PsiNameIdentifierOwner owner && owner.getNameIdentifier() == leaf) {
            return owner;
        }
        return null;
    }

    static String kind(PsiElement element) {
        String kind = ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE).get();
        return kind.isEmpty() ? element.getClass().getSimpleName() : kind;
    }

    static String name(PsiElement element) {
        String qualifiedName = QualifiedNameProviderUtil.getQualifiedName(element);
        if (qualifiedName != null) {
            return qualifiedName;
        }
        if (element instanceof PsiNamedElement named && named.getName() != null) {
            return named.getName();
        }
        return element.getClass().getSimpleName();
    }

    static String location(Project project, PsiElement element) {
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null) {
            return "no file";
        }

        boolean inProject = ProjectFileIndex.getInstance(project).isInContent(virtualFile);
        String path = inProject ? McpProjectPaths.relativePath(project, virtualFile) : virtualFile.getPresentableUrl();

        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        int offset = element.getTextOffset();
        String line = document == null || offset < 0 || offset > document.getTextLength()
            ? ""
            : ":" + (document.getLineNumber(offset) + 1);
        return (inProject ? "" : "library ") + path + line;
    }

    static int lineStartOffset(Document document, int line) {
        int lineIndex = line - 1;
        if (lineIndex < 0 || lineIndex >= document.getLineCount()) {
            return -1;
        }
        return document.getLineStartOffset(lineIndex);
    }

    private McpCodeElements() {
    }
}
