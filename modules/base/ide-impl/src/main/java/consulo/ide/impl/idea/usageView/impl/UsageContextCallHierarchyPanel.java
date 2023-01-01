/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.hierarchy.CallHierarchyBrowserBase;
import consulo.ide.impl.idea.ide.hierarchy.HierarchyBrowserBaseEx;
import consulo.ide.impl.idea.ide.hierarchy.actions.BrowseHierarchyActionBase;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.usages.impl.UsageContextPanelBase;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.hierarchy.CallHierarchyProvider;
import consulo.language.editor.hierarchy.HierarchyBrowser;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewPresentation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UsageContextCallHierarchyPanel extends UsageContextPanelBase {
  private HierarchyBrowser myBrowser;

  public UsageContextCallHierarchyPanel(@Nonnull Project project, @Nonnull UsageViewPresentation presentation) {
    super(project, presentation);
  }

  @Override
  public void dispose() {
    super.dispose();
    myBrowser = null;
  }

  @Override
  public void updateLayoutLater(@Nullable final List<? extends UsageInfo> infos) {
    PsiElement element = infos == null ? null : getElementToSliceOn(infos);
    if (myBrowser instanceof Disposable) {
      Disposer.dispose((Disposable)myBrowser);
      myBrowser = null;
    }
    if (element != null) {
      myBrowser = createCallHierarchyPanel(element);
      if (myBrowser == null) {
        element = null;
      }
    }

    removeAll();
    if (element == null) {
      JComponent titleComp = new JLabel(UsageViewBundle.message("select.the.usage.to.preview", myPresentation.getUsagesWord()), SwingConstants.CENTER);
      add(titleComp, BorderLayout.CENTER);
    }
    else {
      if (myBrowser instanceof Disposable) {
        Disposer.register(this, (Disposable)myBrowser);
      }
      JComponent panel = myBrowser.getComponent();
      add(panel, BorderLayout.CENTER);
    }
    revalidate();
  }

  @Nullable
  private static HierarchyBrowser createCallHierarchyPanel(@Nonnull PsiElement element) {
    DataContext context = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT, element, SimpleDataContext.getProjectContext(element.getProject()));
    CallHierarchyProvider provider = BrowseHierarchyActionBase.findBestHierarchyProvider(CallHierarchyProvider.class, element, context);
    if (provider == null) return null;
    PsiElement providerTarget = provider.getTarget(context);
    if (providerTarget == null) return null;

    HierarchyBrowser browser = provider.createHierarchyBrowser(providerTarget);
    if (browser instanceof HierarchyBrowserBaseEx) {
      HierarchyBrowserBaseEx browserEx = (HierarchyBrowserBaseEx)browser;
      // do not steal focus when scrolling through nodes
      browserEx.changeView(CallHierarchyBrowserBase.CALLER_TYPE, false);
    }
    return browser;
  }

  private static PsiElement getElementToSliceOn(@Nonnull List<? extends UsageInfo> infos) {
    UsageInfo info = infos.get(0);
    return info.getElement();
  }
}
