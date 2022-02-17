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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.impl.DocumentEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import consulo.project.ui.util.Alarm;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;

import javax.annotation.Nonnull;

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
  private final Alarm updateStatusAlarm = new Alarm();

  public StatusBarUpdater(Project project) {
    myProject = project;

    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        updateLater();
      }
    });

    project.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListenerAdapter() {
      @Override
      public void daemonFinished() {
        updateLater();
      }
    });
  }

  private void updateLater() {
    final Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode()) {
      myUpdateStatusRunnable.run();
    }
    else {
      updateStatusAlarm.cancelAllRequests();
      updateStatusAlarm.addRequest(myUpdateStatusRunnable, 100);
    }
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
    if(!FocusableComponent.hasFocus(contentUIComponent)) {
      return;
    }

    final Document document = editor.getDocument();
    if (document instanceof DocumentEx && document.isInBulkUpdate()) return;

    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(myProject);
    HighlightInfoImpl info = ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(document, offset, false, HighlightSeverity.WARNING);
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
