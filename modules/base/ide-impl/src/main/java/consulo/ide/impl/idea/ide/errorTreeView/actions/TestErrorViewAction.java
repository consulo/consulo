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
package consulo.ide.impl.idea.ide.errorTreeView.actions;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.errorTreeView.ErrorTreeView;
import consulo.ui.ex.toolWindow.ToolWindow;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-13
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class TestErrorViewAction extends AnAction{
  private static final int MESSAGE_COUNT = 1000;
  private long myMillis = 0L;
  private int myMessageCount = 0;

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    final ErrorTreeView view = createView(project);
    openView(project, view.getComponent());
    myMillis = 0L;
    myMessageCount = 0;
    new Thread() {
      @Override
      public void run() {
        for (int idx = 0; idx < MESSAGE_COUNT; idx++) {
          addMessage(view, new String[] {"This is a warning test message" + idx + " line1", "This is a warning test message" + idx + " line2"}, MessageCategory.WARNING);
        }
        while (getMessageCount() < MESSAGE_COUNT) {
          try {
            Thread.sleep(100);
          }
          catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
        String statistics = "Duration = " + myMillis;
        addMessage(view, new String[] {statistics}, MessageCategory.STATISTICS);
        System.out.println(statistics);
        while (getMessageCount() < MESSAGE_COUNT + 1) {
          try {
            Thread.sleep(100);
          }
          catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
        System.out.println("Expected " + (MESSAGE_COUNT + 1) + " messages;");
        view.dispose();
      }
    }.start();
  }

  public synchronized int getMessageCount() {
    return myMessageCount;
  }

  public synchronized void incMessageCount() {
    myMessageCount++;
  }

  private void addMessage(final ErrorTreeView view, final String[] message, final int type) {
    Application.get().invokeLater(
      () -> {
        final long start = System.currentTimeMillis();
        view.addMessage(type, message, null, -1, -1, null);
        final long duration = System.currentTimeMillis() - start;
        myMillis += duration;
        incMessageCount();
      },
      IdeaModalityState.nonModal()
    );
  }

  protected abstract ErrorTreeView createView(Project project);
  protected abstract String getContentName();

  @RequiredUIAccess
  protected void openView(Project project, JComponent component) {
    final MessageView messageView = MessageView.getInstance(project);
    final Content content = ContentFactory.getInstance().createContent(component, getContentName(), true);
    messageView.getContentManager().addContent(content);
    messageView.getContentManager().setSelectedContent(content);
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
    if (toolWindow != null) {
      toolWindow.activate(null);
    }
  }
}
