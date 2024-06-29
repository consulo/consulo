/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.usageView.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.hierarchy.actions.BrowseHierarchyActionBase;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.usages.impl.UsageViewImpl;
import consulo.language.editor.hierarchy.CallHierarchyProvider;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.*;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class UsageContextCallHierarchyPanelProvider implements UsageContextPanelProvider {
  @Nonnull
  @Override
  public UsageContextPanel create(@Nonnull UsageView usageView) {
    return new UsageContextCallHierarchyPanel(((UsageViewImpl)usageView).getProject(), usageView.getPresentation());
  }

  @Override
  @RequiredReadAction
  public boolean isAvailableFor(@Nonnull UsageView usageView) {
    UsageTarget[] targets = ((UsageViewImpl)usageView).getTargets();
    if (targets.length == 0) return false;
    UsageTarget target = targets[0];
    if (!(target instanceof PsiElementUsageTarget)) return false;
    PsiElement element = ((PsiElementUsageTarget)target).getElement();
    if (element == null || !element.isValid()) return false;

    Project project = element.getProject();
    DataContext context = SimpleDataContext.getSimpleContext(PsiElement.KEY, element, SimpleDataContext.getProjectContext(project));
    HierarchyProvider provider = BrowseHierarchyActionBase.findBestHierarchyProvider(CallHierarchyProvider.class, element, context);
    if (provider == null) return false;
    PsiElement providerTarget = provider.getTarget(context);
    return providerTarget != null;
  }

  @Nonnull
  @Override
  public String getTabTitle() {
    return "Call Hierarchy";
  }
}
