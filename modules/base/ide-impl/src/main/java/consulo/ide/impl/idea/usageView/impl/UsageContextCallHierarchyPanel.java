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

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.usages.impl.UsageContextPanelBase;
import consulo.language.editor.hierarchy.StandardHierarchyKinds;
import consulo.language.editor.internal.hierarchy.HierarchyBrowseService;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewPresentation;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UsageContextCallHierarchyPanel extends UsageContextPanelBase {
    private HierarchyBrowser myBrowser;

    public UsageContextCallHierarchyPanel(Project project, UsageViewPresentation presentation) {
        super(project, presentation);
    }

    @Override
    public void dispose() {
        super.dispose();
        myBrowser = null;
    }

    @Override
    @RequiredReadAction
    public void updateLayoutLater(@Nullable List<? extends UsageInfo> infos) {
        PsiElement element = infos == null ? null : getElementToSliceOn(infos);
        if (myBrowser != null) {
            Disposer.dispose(myBrowser);
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
            JComponent titleComp = new JLabel(
                UsageViewBundle.message("select.the.usage.to.preview", myPresentation.getUsagesWord()),
                SwingConstants.CENTER
            );
            add(titleComp, BorderLayout.CENTER);
        }
        else {
            Disposer.register(this, myBrowser);
            add(TargetAWT.to(myBrowser.getUIComponent()), BorderLayout.CENTER);
        }
        revalidate();
    }

    @RequiredReadAction
    private static @Nullable HierarchyBrowser createCallHierarchyPanel(PsiElement element) {
        Project project = element.getProject();
        DataContext context =
            SimpleDataContext.getSimpleContext(PsiElement.KEY, element, SimpleDataContext.getProjectContext(project));
        return HierarchyBrowseService.getInstance(project).createBrowser(StandardHierarchyKinds.CALL, context);
    }

    @RequiredReadAction
    private static PsiElement getElementToSliceOn(List<? extends UsageInfo> infos) {
        UsageInfo info = infos.get(0);
        return info.getElement();
    }
}
