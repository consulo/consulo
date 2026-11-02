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
package consulo.language.editor.refactoring.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.refactoring.rename.RenameProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.DumbService;
import consulo.module.content.ProjectFileIndex;
import consulo.language.editor.refactoring.rename.RenameUtil;
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
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class RefactoringToolset implements McpToolset {
    @Override
    public McpToolGroup getGroup() {
        return McpToolGroup.EDITING;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("rename_refactoring")
            .description("Renames the symbol at the given 1-based line and column and updates every reference to it across the project. If "
                + "the position sits on a usage, the declaration it resolves to is renamed. Comments and string literals are left alone. "
                + "This edits many files at once and is undoable by the user.")
            .destructive()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative path of the file holding the symbol."))
            .param(McpParameter.integer("line", "1-based line number of the symbol."))
            .param(McpParameter.integer("column", "1-based column number of the symbol."))
            .param(McpParameter.string("newName", "New name for the symbol."))
            .handler(RefactoringToolset::renameRefactoring);
    }

    private static CompletableFuture<McpToolCallResult> renameRefactoring(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int line = context.getInt("line");
        int column = context.getInt("column");
        String newName = context.getString("newName");

        return McpToolActions.uiAction(project, () -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            if (DumbService.isDumb(project)) {
                return McpToolCallResult.error("The IDE is building its indexes, retry when indexing has finished.");
            }

            PsiNamedElement target = findTarget(project, path, line, column);
            if (target == null) {
                return McpToolCallResult.error("No renamable symbol at " + path + ":" + line + ":" + column
                    + ". Point at the symbol's name or at a reference to it.");
            }
            String error = checkRenamable(project, target, newName);
            if (error != null) {
                return McpToolCallResult.error(error);
            }

            String oldName = target.getName();
            SmartPsiElementPointer<PsiNamedElement> pointer = SmartPointerManager.createPointer(target);
            new RenameProcessor(project, target, newName, false, false).run();

            PsiNamedElement renamed = pointer.getElement();
            if (renamed == null || !newName.equals(renamed.getName())) {
                return McpToolCallResult.error("The rename of '" + oldName + "' did not complete: it was cancelled, blocked by conflicts "
                    + "or read-only files, or the IDE started indexing.");
            }
            return McpToolCallResult.text("Renamed '" + oldName + "' to '" + newName + "'.");
        });
    }

    private static @Nullable String checkRenamable(Project project, PsiNamedElement target, String newName) {
        if (target instanceof PsiCompiledElement || !target.isWritable()) {
            return "'" + target.getName() + "' is read-only or compiled and cannot be renamed.";
        }
        PsiFile containingFile = target.getContainingFile();
        VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
        if (virtualFile == null || !ProjectFileIndex.getInstance(project).isInContent(virtualFile)) {
            return "'" + target.getName() + "' is declared outside the project and cannot be renamed.";
        }
        if (newName.equals(target.getName())) {
            return "The symbol is already called '" + newName + "'.";
        }
        if (!RenameUtil.isValidName(project, target, newName)) {
            return "'" + newName + "' is not a valid name for " + target.getName() + ".";
        }
        return null;
    }

    static @Nullable PsiNamedElement findTarget(Project project, String path, int line, int column) {
        VirtualFile file = McpProjectPaths.resolve(project, path);

        Document document = FileDocumentManager.getInstance().getDocument(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (document == null || psiFile == null) {
            return null;
        }

        int offset = offsetOf(document, line, column);
        if (offset < 0) {
            return null;
        }

        PsiReference reference = psiFile.findReferenceAt(offset);
        if (reference != null) {
            PsiElement resolved = reference.resolve();
            if (resolved instanceof PsiNamedElement named) {
                return named;
            }
        }

        PsiElement leaf = psiFile.findElementAt(offset);
        PsiElement parent = leaf == null ? null : leaf.getParent();
        if (parent instanceof PsiNameIdentifierOwner owner && owner.getNameIdentifier() == leaf) {
            return owner;
        }
        return null;
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
