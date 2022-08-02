package consulo.ide.impl.idea.tasks.actions;

import consulo.application.AllIcons;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.ide.impl.idea.tasks.doc.TaskPsiElement;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherHolder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.task.TasksIcons;

import javax.swing.*;
import java.awt.*;

/**
 * @author Evgeny Zakrevsky
 */
public class TaskCellRenderer extends DefaultListCellRenderer implements MatcherHolder {
  private Matcher myMatcher;
  private final Project myProject;


  public TaskCellRenderer(Project project) {
    super();
    myProject = project;
  }


  public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(UIUtil.getListBackground(sel));
    panel.setForeground(UIUtil.getListForeground(sel));
    if (value instanceof TaskPsiElement) {
      final Task task = ((TaskPsiElement)value).getTask();
      final SimpleColoredComponent c = new SimpleColoredComponent();
      final TaskManager taskManager = TaskManager.getManager(myProject);
      final boolean isLocalTask = taskManager.findTask(task.getId()) != null;
      final boolean isClosed = task.isClosed() || (task instanceof LocalTask && taskManager.isLocallyClosed((LocalTask)task));

      final Color bg = sel ? UIUtil.getListSelectionBackground() : isLocalTask ? UIUtil.getListBackground() : UIUtil.getDecoratedRowColor();
      panel.setBackground(bg);
      SimpleTextAttributes attr = getAttributes(sel, isClosed);

      c.setIcon(isClosed ? ImageEffects.transparent(task.getIcon()) : task.getIcon());
      SpeedSearchUtil.appendColoredFragmentForMatcher(task.getPresentableName(), c, attr, myMatcher, bg, sel);
      panel.add(c, BorderLayout.CENTER);
    }
    else if ("...".equals(value)) {
      final SimpleColoredComponent c = new SimpleColoredComponent();
      c.setIcon(Image.empty(16));
      c.append((String)value);
      panel.add(c, BorderLayout.CENTER);
    }
    else if (GotoTaskAction.CREATE_NEW_TASK_ACTION == value) {
      final SimpleColoredComponent c = new SimpleColoredComponent();
      c.setIcon(ImageEffects.layered(TasksIcons.Unknown, AllIcons.Actions.New));
      c.append(GotoTaskAction.CREATE_NEW_TASK_ACTION.getActionText());
      panel.add(c, BorderLayout.CENTER);
    }
    return panel;
  }

  private static SimpleTextAttributes getAttributes(final boolean selected, final boolean taskClosed) {
    return new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                                    taskClosed ? UIUtil.getLabelDisabledForeground() : UIUtil.getListForeground(selected));
  }

  @Override
  public void setPatternMatcher(Matcher matcher) {
    myMatcher = matcher;
  }
}
