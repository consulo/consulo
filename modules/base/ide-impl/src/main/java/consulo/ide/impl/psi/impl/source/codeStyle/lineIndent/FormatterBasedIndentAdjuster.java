// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.source.codeStyle.lineIndent;

import consulo.language.codeStyle.FormattingMode;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormattingModeAwareIndentAdjuster;
import javax.annotation.Nonnull;

public class FormatterBasedIndentAdjuster {

  private final static int MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE = 100000;

  private FormatterBasedIndentAdjuster() {
  }

  public static void scheduleIndentAdjustment(@Nonnull Project myProject, @Nonnull Document myDocument, int myOffset) {
    IndentAdjusterRunnable fixer = new IndentAdjusterRunnable(myProject, myDocument, myOffset);
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
    if (isSynchronousAdjustment(myDocument)) {
      documentManager.commitDocument(myDocument);
      fixer.run();
    }
    else {
      documentManager.performLaterWhenAllCommitted(fixer);
    }
  }

  private static boolean isSynchronousAdjustment(@Nonnull Document document) {
    return ApplicationManager.getApplication().isUnitTestMode() || document.getTextLength() <= MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE;
  }

  public static class IndentAdjusterRunnable implements Runnable {
    private final Project myProject;
    private final int myLine;
    private final Document myDocument;

    public IndentAdjusterRunnable(Project project, Document document, int offset) {
      myProject = project;
      myDocument = document;
      myLine = myDocument.getLineNumber(offset);
    }

    @Override
    public void run() {
      int lineStart = myDocument.getLineStartOffset(myLine);
      CommandProcessor.getInstance().runUndoTransparentAction(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(myProject);
        if (codeStyleManager instanceof FormattingModeAwareIndentAdjuster) {
          ((FormattingModeAwareIndentAdjuster)codeStyleManager).adjustLineIndent(myDocument, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER);
        }
      }));
    }
  }

}
