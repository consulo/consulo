// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.daemon;

import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.fileEditor.internal.EditorWindowHolder;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopup;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author max
 */
public class DaemonEditorPopup extends PopupHandler {
  private final Project myProject;
  private final Editor myEditor;

  public DaemonEditorPopup(@Nonnull final Project project, @Nonnull final Editor editor) {
    myProject = project;
    myEditor = editor;
  }

  @Override
  public void invokePopup(final Component comp, final int x, final int y) {
    if (ApplicationManager.getApplication() == null) return;
    final PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (file == null) return;

    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    DefaultActionGroup gotoGroup = createGotoGroup();
    actionGroup.add(gotoGroup);
    actionGroup.addSeparator();
    actionGroup.add(new AnAction(CodeEditorLocalize.customizeHighlightingLevelMenuItem()) {
      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        JBPopup popup = ConfigureHighlightingLevel.getConfigureHighlightingLevelPopup(e.getDataContext());
        if (popup != null) popup.show(new RelativePoint(comp, new Point(x, y)));
      }
    });
    if (!UIUtil.uiParents(myEditor.getComponent(), false).filter(EditorWindowHolder.class).isEmpty()) {
      actionGroup.addSeparator();
      actionGroup.add(new ToggleAction(CodeEditorLocalize.checkboxShowEditorPreviewPopup()) {
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
          return UISettings.getInstance().getShowEditorToolTip();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
          UISettings.getInstance().setShowEditorToolTip(state);
          UISettings.getInstance().fireUISettingsChanged();
        }
      });
    }
    ActionPopupMenu editorPopup = actionManager.createActionPopupMenu(ActionPlaces.RIGHT_EDITOR_GUTTER_POPUP, actionGroup);
    if (DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(file)) {
      //UIEventLogger.logUIEvent(UIEventId.DaemonEditorPopupInvoked);
      editorPopup.getComponent().show(comp, x, y);
    }
  }

  @Nonnull
  public static DefaultActionGroup createGotoGroup() {
    Shortcut shortcut = KeymapUtil.getPrimaryShortcut("GotoNextError");
    String shortcutText = shortcut != null ? " (" + KeymapUtil.getShortcutText(shortcut) + ")" : "";
    DefaultActionGroup gotoGroup =
        DefaultActionGroup.createPopupGroup(() -> CodeInsightLocalize.popupTitleNextErrorAction0GoesThrough(shortcutText).get());
    gotoGroup.add(new ToggleAction(CodeEditorLocalize.errorsPanelGoToErrorsFirstRadio()) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
      }

      @Override
      @RequiredUIAccess
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(state);
      }

      @Override
      public boolean isDumbAware() {
        return true;
      }
    });
    gotoGroup.add(new ToggleAction(CodeEditorLocalize.errorsPanelGoToNextErrorWarningRadio()) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return !DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
      }

      @Override
      @RequiredUIAccess
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(!state);
      }

      @Override
      public boolean isDumbAware() {
        return true;
      }
    });
    return gotoGroup;
  }
}