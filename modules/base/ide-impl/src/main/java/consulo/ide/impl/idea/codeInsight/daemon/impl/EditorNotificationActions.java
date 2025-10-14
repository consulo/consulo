// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.language.editor.intention.IntentionActionWithOptions;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionActionProvider;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

class EditorNotificationActions {
  public static void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
    Project project = hostEditor.getProject();
    if (project == null) return;
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    if (!(fileEditorManager instanceof FileEditorManagerImpl)) return;
    TextEditor fileEditor = TextEditorProvider.getInstance().getTextEditor(hostEditor);
    List<JComponent> components = ((FileEditorManagerImpl)fileEditorManager).getTopComponents(fileEditor);
    for (JComponent component : components) {
      if (component instanceof IntentionActionProvider) {
        IntentionActionWithOptions action = ((IntentionActionProvider)component).getIntentionAction();
        if (action != null) {
          intentions.notificationActionsToShow.add(new IntentionActionDescriptor(action, action.getOptions(), LocalizeValue.of()));
        }
      }
    }
  }
}
