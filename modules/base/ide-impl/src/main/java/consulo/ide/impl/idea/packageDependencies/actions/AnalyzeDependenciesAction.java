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

package consulo.ide.impl.idea.packageDependencies.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.ide.impl.idea.analysis.BaseAnalysisAction;
import consulo.ide.impl.idea.analysis.BaseAnalysisActionDialog;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnalyzeDependenciesAction extends BaseAnalysisAction {
  private AnalyzeDependenciesSettingPanel myPanel;

  public AnalyzeDependenciesAction() {
    super(AnalysisScopeBundle.message("action.forward.dependency.analysis"), AnalysisScopeBundle.message("action.analysis.noun"));

  }

  @Override
  protected void analyze(@Nonnull final Project project, AnalysisScope scope) {
    new AnalyzeDependenciesHandler(project, scope, myPanel.myTransitiveCB.isSelected() ? ((SpinnerNumberModel)myPanel.myBorderChooser.getModel()).getNumber().intValue() : 0).analyze();
    myPanel = null;
  }

  @Override
  @Nullable
  protected JComponent getAdditionalActionSettings(final Project project, final BaseAnalysisActionDialog dialog) {
    myPanel = new AnalyzeDependenciesSettingPanel();
    myPanel.myTransitiveCB.setText("Show transitive dependencies. Do not travel deeper than");
    myPanel.myTransitiveCB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        myPanel.myBorderChooser.setEnabled(myPanel.myTransitiveCB.isSelected());
      }
    });
    myPanel.myBorderChooser.setModel(new SpinnerNumberModel(5, 0, Integer.MAX_VALUE, 1));
    myPanel.myBorderChooser.setEnabled(myPanel.myTransitiveCB.isSelected());
    return myPanel.myWholePanel;
  }

  @Override
  protected void canceled() {
    super.canceled();
    myPanel = null;
  }

  private static class AnalyzeDependenciesSettingPanel {
    private JCheckBox myTransitiveCB;
    private JPanel myWholePanel;
    private JSpinner myBorderChooser;
  }

}
