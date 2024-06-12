/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class VcsCommitMessageMarginConfigurable implements UnnamedConfigurable {

  @Nonnull
  private final VcsConfiguration myConfiguration;

  @Nonnull
  private final MySpinnerConfigurable mySpinnerConfigurable;
  @Nonnull
  private final JBCheckBox myWrapCheckbox;

  public VcsCommitMessageMarginConfigurable(@Nonnull Project project, @Nonnull VcsConfiguration vcsConfiguration) {
    myConfiguration = vcsConfiguration;
    mySpinnerConfigurable = new MySpinnerConfigurable(project);
    myWrapCheckbox = new JBCheckBox(ApplicationLocalize.checkboxWrapTypingOnRightMargin().get(), false);
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    JComponent spinnerComponent = mySpinnerConfigurable.createComponent(uiDisposable);
    mySpinnerConfigurable.myHighlightRecentlyChanged.addActionListener(e -> myWrapCheckbox.setEnabled(mySpinnerConfigurable.myHighlightRecentlyChanged.isSelected()));

    JPanel rootPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    rootPanel.add(spinnerComponent);
    rootPanel.add(myWrapCheckbox);
    return rootPanel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return mySpinnerConfigurable.isModified() || myWrapCheckbox.isSelected() != myConfiguration.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    mySpinnerConfigurable.apply();
    myConfiguration.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = myWrapCheckbox.isSelected();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    mySpinnerConfigurable.reset();
    myWrapCheckbox.setSelected(myConfiguration.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN);
    myWrapCheckbox.setEnabled(mySpinnerConfigurable.myHighlightRecentlyChanged.isSelected());
  }

  private class MySpinnerConfigurable extends VcsCheckBoxWithSpinnerConfigurable {

    public MySpinnerConfigurable(Project project) {
      super(project, VcsLocalize.configurationCommitMessageMarginPrompt().get(), "");
    }

    @Override
    protected SpinnerNumberModel createSpinnerModel() {
      final int columns = myConfiguration.COMMIT_MESSAGE_MARGIN_SIZE;
      return new SpinnerNumberModel(columns, 0, 10000, 1);
    }

    @Override
    public boolean isModified() {
      if (myHighlightRecentlyChanged.isSelected() != myConfiguration.USE_COMMIT_MESSAGE_MARGIN) {
        return true;
      }

      return !Comparing.equal(myHighlightInterval.getValue(), myConfiguration.COMMIT_MESSAGE_MARGIN_SIZE);
    }

    @Override
    public void apply() {
      myConfiguration.USE_COMMIT_MESSAGE_MARGIN = myHighlightRecentlyChanged.isSelected();
      myConfiguration.COMMIT_MESSAGE_MARGIN_SIZE = ((Number) myHighlightInterval.getValue()).intValue();
    }

    @Override
    public void reset() {
      myHighlightRecentlyChanged.setSelected(myConfiguration.USE_COMMIT_MESSAGE_MARGIN);
      myHighlightInterval.setValue(myConfiguration.COMMIT_MESSAGE_MARGIN_SIZE);
      myHighlightInterval.setEnabled(myHighlightRecentlyChanged.isSelected());
    }

  }
}
