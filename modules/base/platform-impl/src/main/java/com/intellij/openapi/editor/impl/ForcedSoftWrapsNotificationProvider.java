// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import consulo.annotation.access.RequiredReadAction;
import consulo.editor.internal.EditorInternal;
import consulo.editor.notifications.EditorNotificationProvider;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ForcedSoftWrapsNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("forced.soft.wraps.notification.panel");
  private static final String DISABLED_NOTIFICATION_KEY = "disable.forced.soft.wraps.notification";

  private final Project myProject;

  public ForcedSoftWrapsNotificationProvider(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@Nonnull final VirtualFile file, @Nonnull final FileEditor fileEditor) {
    if (!(fileEditor instanceof TextEditor)) return null;
    final Editor editor = ((TextEditor)fileEditor).getEditor();
    if (!Boolean.TRUE.equals(editor.getUserData(EditorInternal.FORCED_SOFT_WRAPS)) ||
        !Boolean.TRUE.equals(editor.getUserData(EditorInternal.SOFT_WRAPS_EXIST)) ||
        PropertiesComponent.getInstance().isTrueValue(DISABLED_NOTIFICATION_KEY)) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(EditorBundle.message("forced.soft.wrap.message"));
    panel.createActionLabel(EditorBundle.message("forced.soft.wrap.hide.message"), () -> {
      editor.putUserData(EditorInternal.FORCED_SOFT_WRAPS, null);
      EditorNotifications.getInstance(myProject).updateNotifications(file);
    });
    panel.createActionLabel(EditorBundle.message("forced.soft.wrap.dont.show.again.message"), () -> {
      PropertiesComponent.getInstance().setValue(DISABLED_NOTIFICATION_KEY, "true");
      EditorNotifications.getInstance(myProject).updateAllNotifications();
    });
    return panel;
  }
}
