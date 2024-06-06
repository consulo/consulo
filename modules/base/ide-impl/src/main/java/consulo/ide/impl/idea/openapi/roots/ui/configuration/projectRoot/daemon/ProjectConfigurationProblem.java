/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ConfigurationError;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author nik
 */
class ProjectConfigurationProblem extends ConfigurationError {
  private final ProjectStructureProblemDescription myDescription;
  private final Project myProject;

  public ProjectConfigurationProblem(@Nonnull ProjectStructureProblemDescription description, @Nonnull Project project) {
    super(StringUtil.unescapeXml(description.getMessage(true)), computeDescription(description),
          getSettings(project, description.getProblemLevel()).isIgnored(description));
    myDescription = description;
    myProject = project;
  }

  @Nonnull
  private static StructureProblemsSettings getSettings(Project project,
                                                       ProjectStructureProblemDescription.ProblemLevel problemLevel) {
    if (problemLevel == ProjectStructureProblemDescription.ProblemLevel.PROJECT) {
      return StructureProblemsSettings.getProjectInstance(project);
    }
    else {
      return StructureProblemsSettings.getGlobalInstance();
    }
  }

  @Nonnull
  private static String computeDescription(ProjectStructureProblemDescription description) {
    final String descriptionString = description.getDescription();
    return descriptionString != null ? descriptionString : description.getMessage(true);
  }

  @Override
  public void ignore(boolean ignored) {
    super.ignore(ignored);
    getSettings(myProject, myDescription.getProblemLevel()).setIgnored(myDescription, ignored);
  }

  @Override
  public void navigate() {
    myDescription.getPlace().navigate(myProject);
  }

  @Override
  public boolean canBeFixed() {
    return !myDescription.getFixes().isEmpty();
  }

  @Override
  public void fix(final JComponent contextComponent, RelativePoint relativePoint) {
    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ConfigurationErrorQuickFix>(null, myDescription.getFixes()) {
      @Nonnull
      @Override
      public String getTextFor(ConfigurationErrorQuickFix value) {
        return value.getActionName();
      }

      @Override
      public PopupStep onChosen(final ConfigurationErrorQuickFix selectedValue, boolean finalChoice) {
        return doFinalStep(() -> selectedValue.performFix(DataManager.getInstance().getDataContext(contextComponent)));
      }
    }).show(relativePoint);
  }
}
