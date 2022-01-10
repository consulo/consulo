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
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.ui.awt.RelativePoint;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author nik
 */
public abstract class FindUsagesInProjectStructureActionBase extends AnAction implements DumbAware {
  private final JComponent myParentComponent;

  public FindUsagesInProjectStructureActionBase(JComponent parentComponent) {
    super(ProjectBundle.message("find.usages.action.text"), ProjectBundle.message("find.usages.action.text"), AllIcons.Actions.Find);
    registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES).getShortcutSet(), parentComponent);
    myParentComponent = parentComponent;
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    // todo e.getPresentation().setEnabled(isEnabled());
  }

  protected abstract boolean isEnabled();

  @Override
  public void actionPerformed(AnActionEvent e) {
    final ProjectStructureElement selected = getSelectedElement();
    if (selected == null) return;

    //final Collection<ProjectStructureElementUsage> usages = getContext().getDaemonAnalyzer().getUsages(selected);
    //if (usages.isEmpty()) {
    //  Messages.showInfoMessage(myParentComponent, FindBundle.message("find.usage.view.no.usages.text"), FindBundle.message("find.pointcut.applications.not.found.title"));
    //  return;
    //}
    //
    //RelativePoint point = getPointToShowResults();
    //final ProjectStructureElementUsage[] usagesArray = usages.toArray(new ProjectStructureElementUsage[usages.size()]);
    //Arrays.sort(usagesArray, (o1, o2) -> o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName()));
    //
    //BaseListPopupStep<ProjectStructureElementUsage> step =
    //  new BaseListPopupStep<ProjectStructureElementUsage>(ProjectBundle.message("dependencies.used.in.popup.title"), usagesArray) {
    //    @Override
    //    public PopupStep onChosen(final ProjectStructureElementUsage selected, final boolean finalChoice) {
    //      selected.getPlace().navigate(myProject);
    //      return FINAL_CHOICE;
    //    }
    //
    //    @Nonnull
    //    @Override
    //    public String getTextFor(ProjectStructureElementUsage value) {
    //      return value.getPresentableName();
    //    }
    //
    //    @Override
    //    public Image getIconFor(ProjectStructureElementUsage selection) {
    //      return selection.getIcon();
    //    }
    //  };
    //new ListPopupImpl(step) {
    //  @Override
    //  protected ListCellRenderer getListElementRenderer() {
    //    return new ListCellRendererWithRightAlignedComponent<ProjectStructureElementUsage>() {
    //      @Override
    //      protected void customize(ProjectStructureElementUsage value) {
    //        setLeftText(value.getPresentableName());
    //        setIcon(TargetAWT.to(value.getIcon()));
    //        setRightForeground(Color.GRAY);
    //        setRightText(value.getPresentableLocationInElement());
    //      }
    //    };
    //  }
    //}.show(point);
  }

  @Nullable
  protected abstract ProjectStructureElement getSelectedElement();

  protected abstract RelativePoint getPointToShowResults();
}
