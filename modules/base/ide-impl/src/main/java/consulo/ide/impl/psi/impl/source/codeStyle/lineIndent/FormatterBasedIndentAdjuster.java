// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.source.codeStyle.lineIndent;

import consulo.application.Application;
import consulo.document.Document;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.codeStyle.FormattingModeAwareIndentAdjuster;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class FormatterBasedIndentAdjuster {
    private final static int MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE = 100000;

    private FormatterBasedIndentAdjuster() {
    }

    @RequiredUIAccess
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
        return Application.get().isUnitTestMode() || document.getTextLength() <= MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE;
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
        @RequiredUIAccess
        public void run() {
            int lineStart = myDocument.getLineStartOffset(myLine);
            CommandProcessor.getInstance().runUndoTransparentAction(() -> Application.get().runWriteAction(() -> {
                CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(myProject);
                if (codeStyleManager instanceof FormattingModeAwareIndentAdjuster formattingModeAwareIndentAdjuster) {
                    formattingModeAwareIndentAdjuster.adjustLineIndent(myDocument, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER);
                }
            }));
        }
    }
}
