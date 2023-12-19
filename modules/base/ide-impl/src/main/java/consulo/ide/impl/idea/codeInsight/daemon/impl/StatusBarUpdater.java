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

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.language.editor.DaemonListener;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class StatusBarUpdater implements Disposable {
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;
  private final ApplicationConcurrency myApplicationConcurrency;
  private final Project myProject;

  private Future<?> myUpdateStatusAlarm = CompletableFuture.completedFuture(null);

  public StatusBarUpdater(DaemonCodeAnalyzerImpl daemonCodeAnalyzer, ApplicationConcurrency applicationConcurrency, Project project) {
    myDaemonCodeAnalyzer = daemonCodeAnalyzer;
    myApplicationConcurrency = applicationConcurrency;
    myProject = project;

    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
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
    myUpdateStatusAlarm.cancel(false);
    myUpdateStatusAlarm = myApplicationConcurrency.getScheduledExecutorService().schedule(this::updateStatus, 100, TimeUnit.MILLISECONDS);
  }

  @Override
  public void dispose() {
    myUpdateStatusAlarm.cancel(false);
  }

  private void updateStatus() {
    if (myProject.isDisposed()) {
      return;
    }

    Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor(true);
    if (editor == null) {
      return;
    }

    Component contentUIComponent = editor.getContentUIComponent();
    if (!FocusableComponent.hasFocus(contentUIComponent)) {
      return;
    }

    final Document document = editor.getDocument();
    if (document instanceof DocumentEx && document.isInBulkUpdate()) return;

    UIAccess uiAccess = myProject.getUIAccess();
    uiAccess.giveAsync(() -> editor.getCaretModel().getOffset())
            .thenApplyAsync((offset) -> {
              HighlightInfoImpl info = myDaemonCodeAnalyzer.findHighlightByOffset(document, offset, false, HighlightSeverity.WARNING);
              return info != null && info.getDescription() != null ? info.getDescription() : "";
            }, myProject.getApplication().getLock().readExecutor())
            .handleAsync((text, throwable) -> {
              StatusBar statusBar = WindowManager.getInstance().getStatusBar(contentUIComponent, myProject);
              if (statusBar instanceof StatusBarEx) {
                StatusBarEx barEx = (StatusBarEx)statusBar;
                if (!text.equals(barEx.getInfo())) {
                  statusBar.setInfo(text, "updater");
                }
              }
              return null;
            }, uiAccess);
  }
}
