// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.editor.actions.PasteAction;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.QualifiedNameProvider;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.CustomPasteProvider;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.datatransfer.Transferable;
import java.util.function.Supplier;

@ExtensionImpl
public class PasteReferenceProvider implements CustomPasteProvider {
  @Override
  public void performPaste(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    final Editor editor = dataContext.getData(Editor.KEY);
    if (project == null || editor == null) return;

    final String fqn = getCopiedFqn(dataContext);

    QualifiedNameProvider theProvider = null;
    PsiElement element = null;
    for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
      element = provider.qualifiedNameToElement(fqn, project);
      if (element != null) {
        theProvider = provider;
        break;
      }
    }

    if (theProvider != null) {
      insert(fqn, element, editor, theProvider);
    }
  }

  @Override
  public boolean isPastePossible(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    final Editor editor = dataContext.getData(Editor.KEY);
    return project != null && editor != null && getCopiedFqn(dataContext) != null;
  }

  @Override
  public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    String fqn = getCopiedFqn(dataContext);
    return project != null && fqn != null && QualifiedNameProviderUtil.qualifiedNameToElement(fqn, project) != null;
  }

  private static void insert(final String fqn, final PsiElement element, final Editor editor, final QualifiedNameProvider provider) {
    final Project project = editor.getProject();
    if (project == null) return;

    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    documentManager.commitDocument(editor.getDocument());

    final PsiFile file = documentManager.getPsiFile(editor.getDocument());
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      Document document = editor.getDocument();
      documentManager.doPostponedOperationsAndUnblockDocument(document);
      documentManager.commitDocument(document);
      EditorModificationUtil.deleteSelectedText(editor);
      provider.insertQualifiedName(fqn, element, editor, project);
    }), IdeBundle.message("command.pasting.reference"), null);
  }

  @Nullable
  private static String getCopiedFqn(final DataContext context) {
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
