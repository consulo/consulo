/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.actions;

import consulo.execution.ExecutionBundle;
import consulo.execution.executor.Executor;
import consulo.process.KillableProcess;
import consulo.ide.impl.idea.execution.impl.ExecutionManagerImpl;
import consulo.process.ProcessHandler;
import consulo.execution.ui.RunContentDescriptor;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.function.Condition;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.Trinity;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ide.impl.idea.openapi.wm.ex.WindowManagerEx;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ShowRunningListAction extends AnAction {
  public ShowRunningListAction() {
    super(ExecutionBundle.message("show.running.list.action.name"), ExecutionBundle.message("show.running.list.action.description"), null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || project.isDisposed()) return;
    final Ref<Pair<? extends JComponent, String>> stateRef = new Ref<>();
    final Ref<Balloon> balloonRef = new Ref<>();

    final Timer timer = UIUtil.createNamedTimer("runningLists", 250);
    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        Balloon balloon = balloonRef.get();
        if (project.isDisposed() || (balloon != null && balloon.isDisposed())) {
          timer.stop();
          return;
        }
        ArrayList<Project> projects = new ArrayList<>(Arrays.asList(ProjectManager.getInstance().getOpenProjects()));
        //List should begin with current project
        projects.remove(project);
        projects.add(0, project);
        Pair<? extends JComponent, String> state = getCurrentState(projects);

        Pair<? extends JComponent, String> prevState = stateRef.get();
        if (prevState != null && prevState.getSecond().equals(state.getSecond())) return;
        stateRef.set(state);

        BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(state.getFirst());
        builder.setShowCallout(false).setTitle(ExecutionBundle.message("show.running.list.balloon.title")).setBlockClicksThroughBalloon(true).setDialogMode(true).setHideOnKeyOutside(false);
        IdeFrame frame = e.getDataContext().getData(IdeFrame.KEY);
        if (frame == null) {
          frame = WindowManagerEx.getInstance().getIdeFrame(project);
        }
        if (balloon != null) {
          balloon.hide();
        }
        builder.setClickHandler(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof MouseEvent) {
              MouseEvent mouseEvent = (MouseEvent)e.getSource();
              Component component = mouseEvent.getComponent();
              component = SwingUtilities.getDeepestComponentAt(component, mouseEvent.getX(), mouseEvent.getY());
              Object value = ((JComponent)component).getClientProperty(KEY);
              if (value instanceof Trinity) {
                Project aProject = (Project)((Trinity)value).first;
                JFrame aFrame = WindowManager.getInstance().getFrame(aProject);
                if (aFrame != null && !aFrame.isActive()) {
                  IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                    IdeFocusManager.getGlobalInstance().requestFocus(aFrame, true);
                  });
                }
                ExecutionManagerImpl.getInstance(aProject).getContentManager().
                        toFrontRunContent((Executor)((Trinity)value).second, (RunContentDescriptor)((Trinity)value).third);
              }
            }
          }
        }, false);
        balloon = builder.createBalloon();

        balloonRef.set(balloon);
        JComponent component = frame.getComponent();
        RelativePoint point = new RelativePoint(component, new Point(component.getWidth(), 0));
        balloon.show(point, Balloon.Position.below);
      }
    };
    timer.addActionListener(actionListener);
    timer.setInitialDelay(0);
    timer.start();
  }

  private static final Object KEY = new Object();

  private static Pair<? extends JComponent, String> getCurrentState(@Nonnull List<Project> projects) {
    NonOpaquePanel panel = new NonOpaquePanel(new GridLayout(0, 1, 10, 10));
    StringBuilder state = new StringBuilder();
    for (int i = 0; i < projects.size(); i++) {
      Project project = projects.get(i);
      final ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);
      List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(Condition.TRUE);

      if (!runningDescriptors.isEmpty() && projects.size() > 1) {
        state.append(project.getName());
        panel.add(new JLabel("<html><body><b>Project '" + project.getName() + "'</b></body></html>"));
      }

      for (RunContentDescriptor descriptor : runningDescriptors) {
        Set<Executor> executors = executionManager.getExecutors(descriptor);
        for (Executor executor : executors) {
          state.append(System.identityHashCode(descriptor.getAttachedContent())).append("@").append(System.identityHashCode(executor.getIcon())).append(";");
          ProcessHandler processHandler = descriptor.getProcessHandler();
          consulo.ui.image.Image icon = (processHandler instanceof KillableProcess && processHandler.isProcessTerminating()) ? AllIcons.Debugger.KillProcess : executor.getIcon();
          JLabel label = new JLabel("<html><body><a href=\"\">" + descriptor.getDisplayName() + "</a></body></html>", TargetAWT.to(icon), SwingConstants.LEADING);
          label.setIconTextGap(JBUI.scale(2));
          label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          label.putClientProperty(KEY, Trinity.create(project, executor, descriptor));
          panel.add(label);
        }
      }
    }
    if (panel.getComponentCount() == 0) {
      panel.setBorder(JBUI.Borders.empty(10));
      panel.add(new JLabel(ExecutionBundle.message("show.running.list.balloon.nothing"), SwingConstants.CENTER));
    }
    else {
      panel.setBorder(JBUI.Borders.empty(10, 10, 0, 10));
      JLabel label = new JLabel(ExecutionBundle.message("show.running.list.balloon.hint"));
      label.setFont(JBUI.Fonts.miniFont());
      panel.add(label);
    }

    return Pair.create(panel, state.toString());
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      boolean enabled = project != null && !project.isDisposed() && !ExecutionManagerImpl.getInstance(project).getRunningDescriptors(Condition.TRUE).isEmpty();
      e.getPresentation().setEnabled(enabled);
      if (enabled) break;
    }
  }
}
