// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.RealEditor;
import consulo.fileEditor.*;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

@ExtensionImpl
public final class ForcedSoftWrapsNotificationProvider implements EditorNotificationProvider, DumbAware {
  private static final String DISABLED_NOTIFICATION_KEY = "disable.forced.soft.wraps.notification";

  private final Project myProject;

  @Inject
  public ForcedSoftWrapsNotificationProvider(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public String getId() {
    return "forced-soft-wrap";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!(fileEditor instanceof TextEditor)) return null;
    final Editor editor = ((TextEditor)fileEditor).getEditor();
    if (!Boolean.TRUE.equals(editor.getUserData(RealEditor.FORCED_SOFT_WRAPS)) ||
        !Boolean.TRUE.equals(editor.getUserData(RealEditor.SOFT_WRAPS_EXIST)) ||
        ApplicationPropertiesComponent.getInstance().isTrueValue(DISABLED_NOTIFICATION_KEY)) {
      return null;
    }

    EditorNotificationBuilder builder = builderFactory.get();
    builder.withText(LocalizeValue.localizeTODO(EditorBundle.message("forced.soft.wrap.message")));
    builder.withAction(LocalizeValue.localizeTODO(EditorBundle.message("forced.soft.wrap.hide.message")), (i) -> {
      editor.putUserData(RealEditor.FORCED_SOFT_WRAPS, null);
      EditorNotifications.getInstance(myProject).updateNotifications(file);
    });
    builder.withAction(LocalizeValue.localizeTODO(EditorBundle.message("forced.soft.wrap.dont.show.again.message")), (i) -> {
      ApplicationPropertiesComponent.getInstance().setValue(DISABLED_NOTIFICATION_KEY, "true");
      EditorNotifications.getInstance(myProject).updateAllNotifications();
    });
    return builder;
  }
}
