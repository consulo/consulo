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
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.language.editor.CommonDataKeys;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddTestProcessAction extends AnAction implements DumbAware {
  public AddTestProcessAction() {
    super("Add Test Process");
  }

  public void actionPerformed(AnActionEvent e) {

    BalloonBuilder builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("" +
                                                                                                      "Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend " +
                                                                                                      "Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend " +
                                                                                                      "Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend " +
                                                                                                      "Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend " +
                                                                                                      "Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend Load up on guns bring your friends it's fun to loose and to pretend " +
                                                                                                      "", NotificationType.INFO, null);

    JFrame wnd = (JFrame)KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    JRootPane ro = wnd.getRootPane();
    Point point = new Point(ro.getWidth() - 200, ro.getHeight() - 200);
    builder.createBalloon().show(new RelativePoint(ro, point), Balloon.Position.above);


    final Project p = e.getData(CommonDataKeys.PROJECT);
    if (p != null) {
      ToolWindowManager.getInstance(p)
        .notifyByBalloon("TODO", NotificationType.INFO, "Started. <a href=\"#a\">Click me!</a>", null, new HyperlinkListener() {
          public void hyperlinkUpdate(final HyperlinkEvent e) {
            System.out.println(e);
          }
        });
    }

    final Project project = e.getData(CommonDataKeys.PROJECT);
    new Task.Backgroundable(project, "Test Process", true, PerformInBackgroundOption.DEAF) {
      public void run(@Nonnull final ProgressIndicator indicator) {
        try {
          indicator.setText("welcome!");

          Thread.currentThread().sleep(6000);

          countTo(1000, new Count() {
            public void onCount(int each) {

//              if (each == 5) {
//                createAnotherProgress(project);
//              }

              indicator.setText("Found: " + each / 20 + 1);
              if (each / 10.0 == Math.round(each / 10.0)) {
                indicator.setText(null);
              }
              indicator.setFraction(each / 1000.0);

              try {
                Thread.currentThread().sleep(100);
              }
              catch (InterruptedException e1) {
                e1.printStackTrace();
              }

              indicator.checkCanceled();
              indicator.setText2("bla bla bla");
            }
          });
          indicator.stop();
        }
        catch (ProcessCanceledException e1) {
          try {
            Thread.currentThread().sleep(2000);
            indicator.stop();
          }
          catch (InterruptedException e2) {
            e2.printStackTrace();
          }
          return;
        }
        catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }.queue();
  }

  private void createAnotherProgress(final Project project) {
    final Task.Modal task = new Task.Modal(project, "Test2", true/*, PerformInBackgroundOption.DEAF*/) {
      public void run(@Nonnull final ProgressIndicator indicator) {
        try {
          countTo(1000, new Count() {
            public void onCount(int each) {
              indicator.setText("Found: " + each / 20 + 1);
              if (each / 10.0 == Math.round(each / 10.0)) {
                indicator.setText(null);
              }
              indicator.setFraction(each / 1000.0);

              try {
                Thread.currentThread().sleep(100);
              }
              catch (InterruptedException e1) {
                e1.printStackTrace();
              }

              indicator.checkCanceled();
              indicator.setText2("bla bla bla");
            }
          });
          indicator.stop();
        }
        catch (ProcessCanceledException e1) {
          try {
            Thread.currentThread().sleep(2000);
            indicator.stop();
          }
          catch (InterruptedException e2) {
            e2.printStackTrace();
          }
          return;
        }
      }
    };

//    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
//    task.run(indicator != null ? indicator : new EmptyProgressIndicator());

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        task.queue();
      }
    });
  }

  private void countTo(int top, Count count) {
    for (int i = 0; i < top; i++) {
      count.onCount(i);
    }
  }

  private static interface Count {
    void onCount(int each);
  }
}
