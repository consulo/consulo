// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.FileModificationService;
import consulo.ide.IdeBundle;
import consulo.language.editor.QualifiedNameProvider;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.ui.ex.PasteProvider;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.editor.actions.PasteAction;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.idea.util.Producer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.datatransfer.Transferable;

@ExtensionImpl
public class PasteReferenceProvider implements PasteProvider {
  @Override
  public void performPaste(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
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
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    return project != null && editor != null && getCopiedFqn(dataContext) != null;
  }

  @Override
  public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
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
    Producer<Transferable> producer = context.getData(PasteAction.TRANSFERABLE_PROVIDER);

    if (producer != null) {
      Transferable transferable = producer.produce();
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
