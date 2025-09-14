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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author Irina.Chernushina
 * @since 2012-11-28
 */
public abstract class VcsCheckBoxWithSpinnerConfigurable {
  protected final Project myProject;
  private final String myCheckboxText;
  private final String myMeasure;
  protected JCheckBox myHighlightRecentlyChanged;
  protected JSpinner myHighlightInterval;

  public VcsCheckBoxWithSpinnerConfigurable(Project project, String checkboxText, String measure) {
    myProject = project;
    myCheckboxText = checkboxText;
    myMeasure = measure;
  }

  @RequiredUIAccess
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    myHighlightRecentlyChanged = new JCheckBox(myCheckboxText);
    myHighlightInterval = new JSpinner(createSpinnerModel());
    wrapper.add(myHighlightRecentlyChanged);
    wrapper.add(myHighlightInterval);
    JLabel days = new JLabel(myMeasure);
    days.setBorder(JBUI.Borders.empty(0, 1));
    wrapper.add(days);

    myHighlightRecentlyChanged.addActionListener(e -> myHighlightInterval.setEnabled(myHighlightRecentlyChanged.isSelected()));
    return wrapper;
  }

  protected abstract SpinnerNumberModel createSpinnerModel();

  public abstract boolean isModified();

  public abstract void reset();

  public abstract void apply();
}
