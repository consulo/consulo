// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AppUIExecutor;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.AutoPopupControllerImpl;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl(id = "completionAutoPopup", order = "first")
public class CompletionAutoPopupHandler extends TypedHandlerDelegate {
  private static final Logger LOG = Logger.getInstance(CompletionAutoPopupHandler.class);
  public static volatile Key<Boolean> ourTestingAutopopup = Key.create("TestingAutopopup");

  @Nonnull
  @Override
  public Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    LookupEx lookup = LookupManager.getActiveLookup(editor);

    if (LOG.isDebugEnabled()) {
      LOG.debug("checkAutoPopup: character=" + charTyped + ";");
      LOG.debug("phase=" + CompletionServiceImpl.getCompletionPhase());
      LOG.debug("lookup=" + lookup);
      LOG.debug("currentCompletion=" + CompletionServiceImpl.getCompletionService().getCurrentCompletion());
    }

    if (lookup != null) {
      if (editor.getSelectionModel().hasSelection()) {
        lookup.performGuardedChange(() -> EditorModificationUtil.deleteSelectedText(editor));
      }
      return Result.STOP;
    }

    if (Character.isLetterOrDigit(charTyped) || charTyped == '_') {
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return Result.STOP;
    }

    return Result.CONTINUE;
  }
}
