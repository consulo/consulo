/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import com.intellij.compiler.ProblemsView;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.pom.Navigatable;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.compiler.server.rmi.CompilerClientConnector;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/18/12
 */
@Logger
public class ProblemsViewImpl extends ProblemsView {
  private static class TempMessage {
    final int type;
    @NotNull
    final String[] text;
    @Nullable
    final String groupName;
    @Nullable
    final Navigatable navigatable;
    @Nullable
    final String exportTextPrefix;
    @Nullable
    final String rendererTextPrefix;

    private TempMessage(int type,
                        @NotNull String[] text,
                        @Nullable String groupName,
                        @Nullable Navigatable navigatable,
                        @Nullable String exportTextPrefix,
                        @Nullable String rendererTextPrefix) {
      this.type = type;
      this.text = text;
      this.groupName = groupName;
      this.navigatable = navigatable;
      this.exportTextPrefix = exportTextPrefix;
      this.rendererTextPrefix = rendererTextPrefix;
    }
  }

  private static final Key<Boolean> ourViewKey = Key.create("ProblemsViewImpl");

  private List<TempMessage> myTempMessages = new CopyOnWriteArrayList<TempMessage>();

  @Nullable
  private ProblemsViewPanel myPanel;

  private final SequentialTaskExecutor myViewUpdater = new SequentialTaskExecutor(new Executor() {
    @Override
    public void execute(@NotNull Runnable command) {
      ApplicationManager.getApplication().executeOnPooledThread(command);
    }
  });

  public ProblemsViewImpl(final Project project) {
    super(project);
  }

  @Override
  public void clearOldMessages() {
    myTempMessages.clear();

    final ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      myViewUpdater.execute(new Runnable() {
        @Override
        public void run() {
          panel.clearMessages();
        }
      });
    }
  }


  @Override
  public void addMessage(final int type,
                         @NotNull final String[] text,
                         @Nullable final String groupName,
                         @Nullable final Navigatable navigatable,
                         @Nullable final String exportTextPrefix,
                         @Nullable final String rendererTextPrefix) {
    final TempMessage message = new TempMessage(type, text, groupName, navigatable, exportTextPrefix, rendererTextPrefix);

    myTempMessages.add(message);

    final ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      myViewUpdater.execute(new Runnable() {
        @Override
        public void run() {
          addMessage(panel, message);
        }
      });
    }
  }

  @RequiredDispatchThread
  @Override
  public void showOrHide(final boolean hide) {
    if (ApplicationManager.getApplication().isCompilerServerMode()) {
      CompilerClientConnector.getInstance(myProject).showOrHide(hide);
      return;
    }

    ToolWindow toolWindow = MessageView.SERVICE.getInstance(myProject).getToolWindow();
    // dont try hide if toolwindow closed
    if (hide && !toolWindow.isVisible()) {
      return;
    }

    final ContentManager contentManager = toolWindow.getContentManager();
    Content[] contents = contentManager.getContents();
    Content content = ContainerUtil.find(contents, new Condition<Content>() {
      @Override
      public boolean value(Content content) {
        return content.getUserData(ourViewKey) != null;
      }
    });

    if (content == null && hide) {
      return;
    }

    if (hide) {
      contentManager.removeContent(content, true);
    }
    else {
      if (content == null) {
        ProblemsViewPanel problemsViewPanel = new ProblemsViewPanel(myProject);
        for (TempMessage tempMessage : myTempMessages) {
          addMessage(problemsViewPanel, tempMessage);
        }

        content = ContentFactory.SERVICE.getInstance().createContent(myPanel = problemsViewPanel, "Compilation", false);
        content.putUserData(ourViewKey, Boolean.TRUE);

        contentManager.addContent(content);
      }

      contentManager.setSelectedContent(content, true);
      toolWindow.show(EmptyRunnable.getInstance());
    }
  }

  private static void addMessage(ProblemsViewPanel problemsViewPanel, TempMessage tempMessage) {
    if (tempMessage.navigatable != null) {
      problemsViewPanel.addMessage(tempMessage.type, tempMessage.text, tempMessage.groupName, tempMessage.navigatable, tempMessage.exportTextPrefix,
                                   tempMessage.rendererTextPrefix, null);
    }
    else {
      problemsViewPanel.addMessage(tempMessage.type, tempMessage.text, null, -1, -1, null);
    }
  }

  @Override
  public void selectFirstMessage() {
    if (ApplicationManager.getApplication().isCompilerServerMode()) {
      CompilerClientConnector.getInstance(myProject).selectFirstMessage();
      return;
    }
    final ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          panel.selectFirstMessage();
        }
      });
    }
  }

  @Override
  public boolean isHideWarnings() {
    return !ErrorTreeViewConfiguration.getInstance(myProject).SHOW_WARNINGS;
  }

  @Override
  public void setProgress(String text, float fraction) {
    ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      panel.setProgress(text, fraction);
    }
  }

  @Override
  public void setProgress(String text) {
    ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      panel.setProgressText(text);
    }
  }

  @Override
  public void clearProgress() {
    ProblemsViewPanel panel = myPanel;
    if (panel != null) {
      panel.clearProgressData();
    }
  }
}
