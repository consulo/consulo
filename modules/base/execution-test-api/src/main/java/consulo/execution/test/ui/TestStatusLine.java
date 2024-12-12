/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.ui;

import consulo.execution.localize.ExecutionLocalize;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ProgressBarColors;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBProgressBar;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class TestStatusLine extends JPanel {
  private static final SimpleTextAttributes IGNORE_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ProgressBarColors.YELLOW);
  private static final SimpleTextAttributes ERROR_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ProgressBarColors.RED_TEXT);

  protected final JProgressBar myProgressBar = new JBProgressBar();
  protected final SimpleColoredComponent myState = new SimpleColoredComponent();
  private final JPanel myProgressPanel;

  public TestStatusLine() {
    super(new BorderLayout());
    myProgressPanel = new JPanel(new GridBagLayout());
    add(myProgressPanel, BorderLayout.WEST);
    myProgressBar.setMaximum(100);
    myProgressPanel.add(myProgressBar, new GridBagConstraints(0, 0, 0, 0, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                              new Insets(2, 8, 0, 8), 0, 0));
    setStatusColor(ProgressBarColors.GREEN);
    add(myState, BorderLayout.CENTER);
    myState.append(ExecutionLocalize.junitRuningInfoStartingLabel().get());
  }

  public void formatTestMessage(final int testsTotal,
                                final int finishedTestsCount,
                                final int failuresCount,
                                final int ignoredTestsCount,
                                final Long duration,
                                final long endTime) {
    myState.clear();
    if (testsTotal == 0) return;
    if (duration == null || endTime == 0) {
      myState.append(finishedTestsCount + " of " + getTestsTotalMessage(testsTotal) + (failuresCount + ignoredTestsCount > 0 ? ": " : ""));
      appendFailuresAndIgnores(failuresCount, ignoredTestsCount);
      return;
    }
    String result = "";
    if (finishedTestsCount == testsTotal) {
      if (testsTotal > 1 && (failuresCount == 0 && ignoredTestsCount == 0 || failuresCount == testsTotal || ignoredTestsCount == testsTotal)) {
        result = "All ";
      }
    }
    else {
      result = "Stopped. " + finishedTestsCount + " of ";
    }

    result += getTestsTotalMessage(testsTotal);

    if (failuresCount == 0 && ignoredTestsCount == 0) {
      myState.append(result + " passed");
    }
    else if (failuresCount == finishedTestsCount) {
      myState.append(result + " failed", ERROR_ATTRIBUTES);
    }
    else if (ignoredTestsCount == finishedTestsCount) {
      myState.append(result + " ignored", IGNORE_ATTRIBUTES);
    }
    else {
      myState.append(result + " done: ");
      appendFailuresAndIgnores(failuresCount, ignoredTestsCount);
    }
    myState.append(" – " + StringUtil.formatDuration(duration), SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  private static String getTestsTotalMessage(int testsTotal) {
    return testsTotal + " test" + (testsTotal > 1 ? "s" : "");
  }

  private void appendFailuresAndIgnores(int failuresCount, int ignoredTestsCount) {
    if (failuresCount > 0) {
      myState.append(failuresCount + " failed", ERROR_ATTRIBUTES);
    }
    if (ignoredTestsCount > 0) {
      if (failuresCount > 0) {
        myState.append(", ", ERROR_ATTRIBUTES);
      }
      myState.append(ignoredTestsCount + " ignored", IGNORE_ATTRIBUTES);
    }
  }

  public void setStatusColor(Color color) {
    myProgressBar.setForeground(color);
  }

  public Color getStatusColor() {
    return myProgressBar.getForeground();
  }

  public void setFraction(double v) {
    int fraction = (int)(v * 100);
    myProgressBar.setValue(fraction);
  }

  public void setPreferredSize(boolean orientation) {
    final Dimension size = new JBDimension(orientation ? 150 : 450 , -1);
    myProgressPanel.setMaximumSize(size);
    myProgressPanel.setMinimumSize(size);
    myProgressPanel.setPreferredSize(size);
  }

  public void setText(String progressStatus_text) {
    myState.clear();
    myState.append(progressStatus_text);
  }
}
