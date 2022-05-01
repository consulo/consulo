// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.ide.impl.idea.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import consulo.ide.IdeBundle;
import consulo.application.ui.UISettings;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorBundle;
import consulo.ide.impl.idea.openapi.fileEditor.impl.EditorWindowHolder;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopup;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.UIUtil;
import javax.annotation.Nonnull;

import java.awt.*;

public class DaemonEditorPopup extends PopupHandler {
  private final Project myProject;
  private final Editor myEditor;

  DaemonEditorPopup(@Nonnull final Project project, @Nonnull final Editor editor) {
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
    actionGroup.add(new AnAction(EditorBundle.message("customize.highlighting.level.menu.item")) {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        JBPopup popup = ConfigureHighlightingLevel.getConfigureHighlightingLevelPopup(e.getDataContext());
        if (popup != null) popup.show(new RelativePoint(comp, new Point(x, y)));
      }
    });
    if (!UIUtil.uiParents(myEditor.getComponent(), false).filter(EditorWindowHolder.class).isEmpty()) {
      actionGroup.addSeparator();
      actionGroup.add(new ToggleAction(IdeBundle.message("checkbox.show.editor.preview.popup")) {
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
          return UISettings.getInstance().getShowEditorToolTip();
        }

        @Override
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
  static DefaultActionGroup createGotoGroup() {
    Shortcut shortcut = KeymapUtil.getPrimaryShortcut("GotoNextError");
    String shortcutText = shortcut != null ? " (" + KeymapUtil.getShortcutText(shortcut) + ")" : "";
    DefaultActionGroup gotoGroup = DefaultActionGroup.createPopupGroup(() -> CodeInsightBundle.message("popup.title.next.error.action.0.goes.through", shortcutText));
    gotoGroup.add(new ToggleAction(EditorBundle.message("errors.panel.go.to.errors.first.radio")) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
      }

      @Override
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        DaemonCodeAnalyzerSettings.getInstance().setNextErrorActionGoesToErrorsFirst(state);
      }

      @Override
      public boolean isDumbAware() {
        return true;
      }
    });
    gotoGroup.add(new ToggleAction(EditorBundle.message("errors.panel.go.to.next.error.warning.radio")) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return !DaemonCodeAnalyzerSettings.getInstance().isNextErrorActionGoesToErrorsFirst();
      }

      @Override
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