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
package consulo.task.impl.internal.timeTracking;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.task.LocalTask;
import consulo.task.TaskRepository;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author evgeny.zakrevsky
 * @since 2012-12-26
 */
public class SendTimeTrackingInformationDialog extends DialogWrapper {
  private final static Logger LOG = Logger.getInstance(SendTimeTrackingInformationDialog.class);
  public static final Pattern PATTERN = Pattern.compile("([0-9]+)d ([0-9]+)h ([0-9]+)m");

  @Nullable
  private final Project myProject;
  private final LocalTask myTask;
  private JRadioButton myFromPreviousPostRadioButton;
  private JRadioButton myTotallyRadioButton;
  private JRadioButton myCustomRadioButton;
  private JTextField myFromPreviousPostTextField;
  private JTextField myTotallyTextField;
  private JTextField myCustomTextField;
  private JTextArea myCommentTextArea;
  private JPanel myPanel;
  private JLabel myTaskNameLabel;

  protected SendTimeTrackingInformationDialog(@Nullable Project project, LocalTask localTask) {
    super(project);
    myProject = project;
    myTask = localTask;
    setTitle("Time Tracking");

    myTaskNameLabel.setText(myTask.getPresentableName());
    myFromPreviousPostRadioButton.setSelected(true);
    if (myTask.getLastPost() == null) {
      myFromPreviousPostRadioButton.setVisible(false);
      myFromPreviousPostTextField.setVisible(false);
      myTotallyRadioButton.setSelected(true);
    }
    myFromPreviousPostTextField.setText(formatDuration(myTask.getTimeSpentFromLastPost()));
    myTotallyTextField.setText(formatDuration(myTask.getTotalTimeSpent()));

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  private static String formatDuration(long milliseconds) {
    int second = 1000;
    int minute = 60 * second;
    int hour = 60 * minute;
    int day = 24 * hour;

    int days = (int)(milliseconds / day);
    int hours = (int)(milliseconds % day / hour);
    int minutes = (int)(milliseconds % hour / minute);

    String daysString = days + "d ";
    String hoursString = hours + "h ";
    String minutesString = minutes + "m";

    return daysString + hoursString + minutesString;
  }

  @Override
  protected void doOKAction() {
    String timeSpentText = myFromPreviousPostRadioButton.isSelected() ? myFromPreviousPostTextField.getText()
                           : myTotallyRadioButton.isSelected() ? myTotallyTextField.getText() : myCustomTextField.getText();
    Matcher matcher = PATTERN.matcher(timeSpentText);
    if (matcher.matches()) {
      int timeSpent = Integer.valueOf(matcher.group(1)) * 24 * 60 + Integer.valueOf(matcher.group(2)) * 60 + Integer.valueOf(
        matcher.group(3));

      TaskRepository repository = myTask.getRepository();
      if (repository != null &&
          repository.isSupported(TaskRepository.TIME_MANAGEMENT)) {
        try {
          repository.updateTimeSpent(myTask, timeSpentText, myCommentTextArea.getText());
          myTask.setLastPost(new Date());
        }
        catch (Exception e1) {
          Messages
            .showErrorDialog(myProject, "<html>Could not send information for " + myTask.getPresentableName() + "<br/>" + e1.getMessage(),
                             "Error");
          LOG.warn(e1);
        }
      }
    }


    super.doOKAction();
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    String timeSpentText = myFromPreviousPostRadioButton.isSelected() ? myFromPreviousPostTextField.getText()
                                                                      : myTotallyRadioButton.isSelected() ? myTotallyTextField.getText() : myCustomTextField.getText();
    if (!PATTERN.matcher(timeSpentText).matches()) return new ValidationInfo("Time Spent has broken format");
    return null;
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return "consulo.task.impl.internal.timeTracking.TasksToolWindowPanel";
  }
}
