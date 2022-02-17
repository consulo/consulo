// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.ReferenceImporter;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.HintAction;
import com.intellij.injected.editor.EditorWindow;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.ui.ex.action.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.TransactionGuard;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import consulo.application.progress.ProgressIndicator;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowAutoImportPass extends TextEditorHighlightingPass {
  private final Editor myEditor;

  private final PsiFile myFile;

  private final int myStartOffset;
  private final int myEndOffset;
  private final boolean hasDirtyTextRange;

  ShowAutoImportPass(@Nonnull Project project, @Nonnull final PsiFile file, @Nonnull Editor editor) {
    super(project, editor.getDocument(), false);
    ApplicationManager.getApplication().assertIsDispatchThread();

    myEditor = editor;

    TextRange range = VisibleHighlightingPassFactory.calculateVisibleRange(myEditor);
    myStartOffset = range.getStartOffset();
    myEndOffset = range.getEndOffset();

    myFile = file;

    hasDirtyTextRange = FileStatusMap.getDirtyTextRange(editor, Pass.UPDATE_ALL) != null;
  }

  @Override
  public void doCollectInformation(@Nonnull ProgressIndicator progress) {
  }

  @Override
  public void doApplyInformationToEditor() {
    TransactionGuard.submitTransaction(myProject, this::showImports);
  }

  private void showImports() {
    Application application = ApplicationManager.getApplication();
    application.assertIsDispatchThread();
    if (!application.isHeadlessEnvironment() && !myEditor.getContentComponent().hasFocus()) return;
    if (DumbService.isDumb(myProject) || !myFile.isValid()) return;
    if (myEditor.isDisposed() || myEditor instanceof EditorWindow && !((EditorWindow)myEditor).isValid()) return;

    int caretOffset = myEditor.getCaretModel().getOffset();
    importUnambiguousImports(caretOffset);
    List<HighlightInfoImpl> visibleHighlights = getVisibleHighlights(myStartOffset, myEndOffset, myProject, myEditor, hasDirtyTextRange);

    for (int i = visibleHighlights.size() - 1; i >= 0; i--) {
      HighlightInfoImpl info = visibleHighlights.get(i);
      if (info.startOffset <= caretOffset && showAddImportHint(info)) return;
    }

    for (HighlightInfoImpl visibleHighlight : visibleHighlights) {
      if (visibleHighlight.startOffset > caretOffset && showAddImportHint(visibleHighlight)) return;
    }
  }

  private void importUnambiguousImports(final int caretOffset) {
    if (!DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled()) return;
    if (!DaemonCodeAnalyzer.getInstance(myProject).isImportHintsEnabled(myFile)) return;

    Document document = myEditor.getDocument();
    final List<HighlightInfoImpl> infos = new ArrayList<>();
    DaemonCodeAnalyzerEx.processHighlights(document, myProject, null, 0, document.getTextLength(), i -> {
      HighlightInfoImpl info = (HighlightInfoImpl)i;

      if (info.hasHint() && info.getSeverity() == HighlightSeverity.ERROR && !info.getFixTextRange().containsOffset(caretOffset)) {
        infos.add(info);
      }
      return true;
    });

    List<ReferenceImporter> importers = ReferenceImporter.EP_NAME.getExtensionList();
    for (HighlightInfoImpl info : infos) {
      for (HintAction action : extractHints(info)) {
        if (action.isAvailable(myProject, myEditor, myFile) && action.fixSilently(myEditor)) {
          break;
        }
      }
      for (ReferenceImporter importer : importers) {
        //noinspection deprecation
        if (importer.autoImportReferenceAt(myEditor, myFile, info.getActualStartOffset())) break;
      }
    }
  }

  @Nonnull
  private static List<HighlightInfoImpl> getVisibleHighlights(final int startOffset, final int endOffset, @Nonnull Project project, @Nonnull Editor editor, boolean isDirty) {
    final List<HighlightInfoImpl> highlights = new ArrayList<>();
    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzerEx.processHighlights(editor.getDocument(), project, null, startOffset, endOffset, i -> {
      HighlightInfoImpl info = (HighlightInfoImpl)i;
      //no changes after escape => suggest imports under caret only
      if (!isDirty && !info.getFixTextRange().contains(offset)) {
        return true;
      }
      if (info.hasHint() && !editor.getFoldingModel().isOffsetCollapsed(info.startOffset)) {
        highlights.add(info);
      }
      return true;
    });
    return highlights;
  }

  private boolean showAddImportHint(@Nonnull HighlightInfoImpl info) {
    if (!DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled()) return false;
    if (!DaemonCodeAnalyzer.getInstance(myProject).isImportHintsEnabled(myFile)) return false;
    PsiElement element = myFile.findElementAt(info.startOffset);
    if (element == null || !element.isValid()) return false;

    for (HintAction action : extractHints(info)) {
      if (action.isAvailable(myProject, myEditor, myFile) && action.showHint(myEditor)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  private static List<HintAction> extractHints(@Nonnull HighlightInfoImpl info) {
    List<Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange>> list = info.quickFixActionRanges;
    if (list == null) return Collections.emptyList();

    List<HintAction> hintActions = new SmartList<>();
    for (Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange> pair : list) {
      IntentionAction action = pair.getFirst().getAction();
      if (action instanceof HintAction) {
        hintActions.add((HintAction)action);
      }
    }
    return hintActions;
  }


  @Nonnull
  public static String getMessage(final boolean multiple, @Nonnull String name) {
    final String messageKey = multiple ? "import.popup.multiple" : "import.popup.text";
    String hintText = DaemonBundle.message(messageKey, name);
    hintText += " " + KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    return hintText;
  }
}
