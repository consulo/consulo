/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import consulo.language.editor.hint.HintManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.document.Document;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

public class GotoNextErrorHandler implements CodeInsightActionHandler {
  private final boolean myGoForward;

  public GotoNextErrorHandler(boolean goForward) {
    myGoForward = goForward;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    int caretOffset = editor.getCaretModel().getOffset();
    gotoNextError(project, editor, file, caretOffset);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private void gotoNextError(Project project, Editor editor, PsiFile file, int caretOffset) {
    final SeverityRegistrar severityRegistrar = SeverityRegistrarImpl.getSeverityRegistrar(project);
    DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
    int maxSeverity = settings.NEXT_ERROR_ACTION_GOES_TO_ERRORS_FIRST ? severityRegistrar.getSeveritiesCount() - 1 : 0;

    for (int idx = maxSeverity; idx >= 0; idx--) {
      final HighlightSeverity minSeverity = severityRegistrar.getSeverityByIndex(idx);
      HighlightInfoImpl infoToGo = findInfo(project, editor, caretOffset, minSeverity);
      if (infoToGo != null) {
        navigateToError(project, editor, infoToGo);
        return;
      }
    }
    showMessageWhenNoHighlights(project, file, editor);
  }

  private HighlightInfoImpl findInfo(Project project, Editor editor, final int caretOffset, HighlightSeverity minSeverity) {
    final Document document = editor.getDocument();
    final HighlightInfoImpl[][] infoToGo = new HighlightInfoImpl[2][2]; //HighlightInfo[luck-noluck][skip-noskip]
    final int caretOffsetIfNoLuck = myGoForward ? -1 : document.getTextLength();

    DaemonCodeAnalyzerEx.processHighlights(document, project, minSeverity, 0, document.getTextLength(), new Processor<HighlightInfo>() {
      @Override
      public boolean process(HighlightInfo info) {
        int startOffset = getNavigationPositionFor(info, document);
        if (SeverityRegistrarImpl.isGotoBySeverityEnabled(info.getSeverity())) {
          infoToGo[0][0] = getBetterInfoThan(infoToGo[0][0], caretOffset, startOffset, info);
          infoToGo[1][0] = getBetterInfoThan(infoToGo[1][0], caretOffsetIfNoLuck, startOffset, info);
        }
        infoToGo[0][1] = getBetterInfoThan(infoToGo[0][1], caretOffset, startOffset, info);
        infoToGo[1][1] = getBetterInfoThan(infoToGo[1][1], caretOffsetIfNoLuck, startOffset, info);
        return true;
      }
    });
    if (infoToGo[0][0] == null) infoToGo[0][0] = infoToGo[1][0];
    if (infoToGo[0][1] == null) infoToGo[0][1] = infoToGo[1][1];
    if (infoToGo[0][0] == null) infoToGo[0][0] = infoToGo[0][1];
    return infoToGo[0][0];
  }

  private HighlightInfoImpl getBetterInfoThan(HighlightInfo infoToGo, int caretOffset, int startOffset, HighlightInfo info) {
    if (isBetterThan(infoToGo, caretOffset, startOffset)) {
      infoToGo = info;
    }
    return (HighlightInfoImpl)infoToGo;
  }

  private boolean isBetterThan(HighlightInfo oldInfo, int caretOffset, int newOffset) {
    if (oldInfo == null) return true;
    int oldOffset = getNavigationPositionFor(oldInfo, oldInfo.getHighlighter().getDocument());
    if (myGoForward) {
      return caretOffset < oldOffset != caretOffset < newOffset ? caretOffset < newOffset : newOffset < oldOffset;
    }
    else {
      return caretOffset <= oldOffset != caretOffset <= newOffset ? caretOffset > newOffset : newOffset > oldOffset;
    }
  }

  static void showMessageWhenNoHighlights(Project project, PsiFile file, Editor editor) {
    DaemonCodeAnalyzerImpl codeHighlighter = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    String message = codeHighlighter.isErrorAnalyzingFinished(file)
                     ? InspectionsBundle.message("no.errors.found.in.this.file")
                     : InspectionsBundle.message("error.analysis.is.in.progress");
    HintManager.getInstance().showInformationHint(editor, message);
  }

  static void navigateToError(Project project, final Editor editor, HighlightInfoImpl info) {
    int oldOffset = editor.getCaretModel().getOffset();

    final int offset = getNavigationPositionFor(info, editor.getDocument());
    final int endOffset = info.getActualEndOffset();

    final ScrollingModel scrollingModel = editor.getScrollingModel();
    if (offset != oldOffset) {
      ScrollType scrollType = offset > oldOffset ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;
      editor.getSelectionModel().removeSelection();
      editor.getCaretModel().removeSecondaryCarets();
      editor.getCaretModel().moveToOffset(offset);
      scrollingModel.scrollToCaret(scrollType);
    }

    scrollingModel.runActionOnScrollingFinished(
            new Runnable(){
              @Override
              public void run() {
                int maxOffset = editor.getDocument().getTextLength() - 1;
                if (maxOffset == -1) return;
                scrollingModel.scrollTo(editor.offsetToLogicalPosition(Math.min(maxOffset, endOffset)), ScrollType.MAKE_VISIBLE);
                scrollingModel.scrollTo(editor.offsetToLogicalPosition(Math.min(maxOffset, offset)), ScrollType.MAKE_VISIBLE);
              }
            }
    );

    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
  }

  private static int getNavigationPositionFor(HighlightInfo info, Document document) {
    int start = info.getActualStartOffset();
    if (start >= document.getTextLength()) return document.getTextLength();
    char c = document.getCharsSequence().charAt(start);
    int shift = ((HighlightInfoImpl)info).isAfterEndOfLine() && c != '\n' ? 1 : ((HighlightInfoImpl)info).getNavigationShift();

    int offset = info.getActualStartOffset() + shift;
    return Math.min(offset, document.getTextLength());
  }
}
