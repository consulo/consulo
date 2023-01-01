/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.ui.view;

import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.event.FileEditorManagerAdapter;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.Project;
import consulo.ui.ex.awt.AutoScrollFromSourceHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 02-Apr-22
 */
public abstract class ProjectViewAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
  public ProjectViewAutoScrollFromSourceHandler(@Nonnull Project project, @Nonnull JComponent view) {
    super(project, view);
  }

  public ProjectViewAutoScrollFromSourceHandler(@Nonnull Project project, @Nonnull JComponent view, @Nullable Disposable parentDisposable) {
    super(project, view, parentDisposable);
  }

  protected abstract void selectElementFromEditor(@Nonnull FileEditor editor);

  @Override
  public void install() {
    final MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
    connection.subscribe(FileEditorManagerListener.class, new FileEditorManagerAdapter() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        final FileEditor editor = event.getNewEditor();
        if (editor != null && myComponent.isShowing() && isAutoScrollEnabled()) {
          myAlarm.cancelAllRequests();
          myAlarm.addRequest(() -> selectElementFromEditor(editor), getAlarmDelay(), getModalityState());
        }
      }
    });
  }
}
