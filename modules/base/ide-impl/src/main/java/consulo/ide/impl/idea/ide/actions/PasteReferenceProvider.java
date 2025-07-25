// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.codeEditor.impl.internal.action.PasteAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.QualifiedNameProvider;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CustomPasteProvider;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.datatransfer.Transferable;
import java.util.function.Supplier;

@ExtensionImpl
public class PasteReferenceProvider implements CustomPasteProvider {
    @Override
    @RequiredUIAccess
    public void performPaste(@Nonnull DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        Editor editor = dataContext.getData(Editor.KEY);
        if (project == null || editor == null) {
            return;
        }

        String fqn = getCopiedFqn(dataContext);

        Pair<PsiElement, QualifiedNameProvider> elemProvider = project.getApplication().getExtensionPoint(QualifiedNameProvider.class)
            .computeSafeIfAny(provider -> {
                PsiElement element = provider.qualifiedNameToElement(fqn, project);
                return element != null ? Pair.create(element, provider) : null;
            });

        if (elemProvider != null) {
            insert(fqn, elemProvider.getFirst(), editor, elemProvider.getSecond());
        }
    }

    @Override
    public boolean isPastePossible(@Nonnull DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        Editor editor = dataContext.getData(Editor.KEY);
        return project != null && editor != null && getCopiedFqn(dataContext) != null;
    }

    @Override
    public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        String fqn = getCopiedFqn(dataContext);
        return project != null && fqn != null && QualifiedNameProviderUtil.qualifiedNameToElement(fqn, project) != null;
    }

    @RequiredUIAccess
    private static void insert(String fqn, PsiElement element, Editor editor, QualifiedNameProvider provider) {
        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = editor.getDocument();
        documentManager.commitDocument(document);

        PsiFile file = documentManager.getPsiFile(document);
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(document)
            .name(IdeLocalize.commandPastingReference())
            .inWriteAction()
            .run(() -> {
                documentManager.doPostponedOperationsAndUnblockDocument(document);
                documentManager.commitDocument(document);
                EditorModificationUtil.deleteSelectedText(editor);
                provider.insertQualifiedName(fqn, element, editor, project);
            });
    }

    @Nullable
    private static String getCopiedFqn(DataContext context) {
        Supplier<Transferable> producer = context.getData(PasteAction.TRANSFERABLE_PROVIDER);

        if (producer != null) {
            Transferable transferable = producer.get();
            if (transferable != null) {
                try {
                    return (String)transferable.getTransferData(CopyReferenceAction.ourFlavor);
                }
                catch (Exception ignored) {
                }
            }
            return null;
        }

        return CopyPasteManager.getInstance().getContents(CopyReferenceAction.ourFlavor);
    }
}
