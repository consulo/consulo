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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureDaemonAnalyzer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemType;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemsHolderImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class ProjectStructureElementRenderer extends ColoredTreeCellRenderer {
  private ProjectStructureDaemonAnalyzer myProjectStructureDaemonAnalyzer;

  public ProjectStructureElementRenderer(@Nullable ProjectStructureDaemonAnalyzer projectStructureDaemonAnalyzer) {
    myProjectStructureDaemonAnalyzer = projectStructureDaemonAnalyzer;
  }

  @RequiredUIAccess
  @Override
  public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    if (value instanceof MasterDetailsComponent.MyNode) {
      MasterDetailsComponent.MyNode node = (MasterDetailsComponent.MyNode)value;

      MasterDetailsConfigurable namedConfigurable = node.getConfigurable();
      if (namedConfigurable == null) {
        return;
      }

      LocalizeValue displayName = node.getDisplayName();
      Image icon = namedConfigurable.getIcon();
      setIcon(icon);
      setToolTipText(null);
      setFont(UIUtil.getTreeFont());

      SimpleTextAttributes textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
      if (node.isDisplayInBold()) {
        textAttributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
      }
      else if (namedConfigurable instanceof ProjectStructureElementConfigurable) {
        ProjectStructureElement projectStructureElement = ((ProjectStructureElementConfigurable)namedConfigurable).getProjectStructureElement();
        if (projectStructureElement != null) {
          ProjectStructureDaemonAnalyzer daemonAnalyzer = myProjectStructureDaemonAnalyzer;
          ProjectStructureProblemsHolderImpl problemsHolder = daemonAnalyzer == null ? null : daemonAnalyzer.getProblemsHolder(projectStructureElement);
          if (problemsHolder != null && problemsHolder.containsProblems()) {
            boolean isUnused = problemsHolder.containsProblems(ProjectStructureProblemType.Severity.UNUSED);
            boolean haveWarnings = problemsHolder.containsProblems(ProjectStructureProblemType.Severity.WARNING);
            boolean haveErrors = problemsHolder.containsProblems(ProjectStructureProblemType.Severity.ERROR);
            Color foreground = isUnused ? UIUtil.getInactiveTextColor() : null;
            int style = haveWarnings || haveErrors ? SimpleTextAttributes.STYLE_WAVED : -1;
            Color waveColor = haveErrors ? JBColor.RED : haveWarnings ? JBColor.GRAY : null;
            textAttributes = textAttributes.derive(style, foreground, null, waveColor);
            setToolTipText(problemsHolder.composeTooltipMessage());
          }

          append(displayName, textAttributes);
          String description = projectStructureElement.getDescription();
          if (description != null) {
            append(" (" + description + ")", SimpleTextAttributes.GRAY_ATTRIBUTES, false);
          }
          return;
        }
      }
      append(displayName, textAttributes);
    }
  }
}
