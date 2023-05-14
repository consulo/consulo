// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.intention.IntentionActionWithOptions;
import consulo.language.editor.internal.intention.IntentionActionProvider;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

class EditorNotificationActions {
  public static void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
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
          intentions.notificationActionsToShow.add(new HighlightInfoImpl.IntentionActionDescriptor(action, action.getOptions(), null));
        }
      }
    }
  }
}
