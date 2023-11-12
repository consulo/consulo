/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.dumb.DumbAwareRunnable;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerAdapter;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonListener;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class StatusBarUpdater implements Disposable {
  private final Project myProject;
  private final DumbAwareRunnable myUpdateStatusRunnable = new DumbAwareRunnable() {
    @Override
    public void run() {
      if (!myProject.isDisposed()) {
        updateStatus();
      }
    }
  };

  private Future<?> updateStatusAlarm = CompletableFuture.completedFuture(null);

  public StatusBarUpdater(Project project) {
    myProject = project;

    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.class, new FileEditorManagerAdapter() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        updateLater();
      }
    });

    project.getMessageBus().connect(this).subscribe(DaemonListener.class, new DaemonListener() {
      @Override
      public void daemonFinished() {
        updateLater();
      }
    });
  }

  private void updateLater() {
    updateStatusAlarm.cancel(false);
    updateStatusAlarm = myProject.getUIAccess().getScheduler().schedule(myUpdateStatusRunnable, 100, TimeUnit.MILLISECONDS);
  }

  @Override
  public void dispose() {
  }

  private void updateStatus() {
    if (myProject.isDisposed()) {
      return;
    }

    Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    Component contentUIComponent = editor.getContentUIComponent();
    if (!FocusableComponent.hasFocus(contentUIComponent)) {
      return;
    }

    final Document document = editor.getDocument();
    if (document instanceof DocumentEx && document.isInBulkUpdate()) return;

    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(myProject);
    HighlightInfoImpl info =
      ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(document, offset, false, HighlightSeverity.WARNING);
    String text = info != null && info.getDescription() != null ? info.getDescription() : "";

    StatusBar statusBar = WindowManager.getInstance().getStatusBar(contentUIComponent, myProject);
    if (statusBar instanceof StatusBarEx) {
      StatusBarEx barEx = (StatusBarEx)statusBar;
      if (!text.equals(barEx.getInfo())) {
        statusBar.setInfo(text, "updater");
      }
    }
  }
}
