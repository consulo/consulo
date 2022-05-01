/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.module.content.ProjectTopics;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.AsyncEditorLoader;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.impl.internal.progress.ReadTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.notifications.EditorNotificationProvider;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author peter
 */
public class EditorNotificationsImpl extends EditorNotifications {
  private static final Key<WeakReference<ProgressIndicator>> CURRENT_UPDATES = Key.create("CURRENT_UPDATES");
  private static final ExecutorService ourExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("EditorNotificationsImpl pool");
  private final MergingUpdateQueue myUpdateMerger;
  private final Project myProject;

  @Inject
  public EditorNotificationsImpl(Project project) {
    myProject = project;
    myUpdateMerger = new MergingUpdateQueue("EditorNotifications update merger", 100, true, null, project);
    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        updateNotifications(file);
      }
    });
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void enteredDumbMode() {
        updateAllNotifications();
      }

      @Override
      public void exitDumbMode() {
        updateAllNotifications();
      }
    });
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        updateAllNotifications();
      }
    });
  }

  @Override
  public void updateNotifications(@Nonnull final VirtualFile file) {
    UIUtil.invokeLaterIfNeeded(() -> {
      ProgressIndicator indicator = getCurrentProgress(file);
      if (indicator != null) {
        indicator.cancel();
      }
      file.putUserData(CURRENT_UPDATES, null);

      if (myProject.isDisposed() || !file.isValid()) {
        return;
      }

      indicator = new ProgressIndicatorBase();
      final ReadTask task = createTask(indicator, file);
      if (task == null) return;

      file.putUserData(CURRENT_UPDATES, new WeakReference<>(indicator));
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        ReadTask.Continuation continuation = task.performInReadAction(indicator);
        if (continuation != null) {
          continuation.getAction().run();
        }
      }
      else {
        ProgressIndicatorUtils.scheduleWithWriteActionPriority(indicator, ourExecutor, task);
      }
    });
  }

  @Nullable
  private ReadTask createTask(@Nonnull final ProgressIndicator indicator, @Nonnull final VirtualFile file) {
    List<FileEditor> editors = ContainerUtil.filter(FileEditorManager.getInstance(myProject).getAllEditors(file),
                                                    editor -> !(editor instanceof TextEditor) || AsyncEditorLoader.isEditorLoaded(((TextEditor)editor).getEditor()));

    if (editors.isEmpty()) return null;

    return new ReadTask() {
      private boolean isOutdated() {
        if (myProject.isDisposed() || !file.isValid() || indicator != getCurrentProgress(file)) {
          return true;
        }

        for (FileEditor editor : editors) {
          if (!editor.isValid()) {
            return true;
          }
        }

        return false;
      }

      @RequiredReadAction
      @Nullable
      @Override
      public Continuation performInReadAction(@Nonnull ProgressIndicator indicator) throws ProcessCanceledException {
        if (isOutdated()) return null;

        final List<EditorNotificationProvider<?>> providers = DumbService.getInstance(myProject).filterByDumbAwareness(EditorNotificationProvider.EP_NAME.getExtensionList(myProject));

        final List<Runnable> updates = ContainerUtil.newArrayList();
        for (final FileEditor editor : editors) {
          for (final EditorNotificationProvider<?> provider : providers) {
            final JComponent component = provider.createNotificationPanel(file, editor);
            updates.add(() -> updateNotification(editor, provider.getKey(), component));
          }
        }

        return new Continuation(() -> {
          if (!isOutdated()) {
            file.putUserData(CURRENT_UPDATES, null);
            for (Runnable update : updates) {
              update.run();
            }
          }
        }, IdeaModalityState.any());
      }

      @Override
      public void onCanceled(@Nonnull ProgressIndicator ignored) {
        if (getCurrentProgress(file) == indicator) {
          updateNotifications(file);
        }
      }
    };
  }

  private static ProgressIndicator getCurrentProgress(VirtualFile file) {
    return SoftReference.dereference(file.getUserData(CURRENT_UPDATES));
  }


  private void updateNotification(@Nonnull FileEditor editor, @Nonnull Key<? extends JComponent> key, @Nullable JComponent component) {
    JComponent old = editor.getUserData(key);
    if (old != null) {
      FileEditorManager.getInstance(myProject).removeTopComponent(editor, old);
    }
    if (component != null) {
      FileEditorManager.getInstance(myProject).addTopComponent(editor, component);
      @SuppressWarnings("unchecked") Key<JComponent> _key = (Key<JComponent>)key;
      editor.putUserData(_key, component);
    }
    else {
      editor.putUserData(key, null);
    }
  }

  @Override
  public void updateAllNotifications() {
    myUpdateMerger.queue(new Update("update") {
      @Override
      public void run() {
        for (VirtualFile file : FileEditorManager.getInstance(myProject).getOpenFiles()) {
          updateNotifications(file);
        }
      }
    });
  }
}
