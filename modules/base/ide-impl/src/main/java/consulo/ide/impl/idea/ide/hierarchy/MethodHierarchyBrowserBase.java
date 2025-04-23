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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.application.Application;
import consulo.ide.IdeBundle;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.hierarchy.MethodHierarchyProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public abstract class MethodHierarchyBrowserBase extends HierarchyBrowserBaseEx {
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String METHOD_TYPE = IdeBundle.message("title.hierarchy.method");

    public static final Key<MethodHierarchyBrowserBase> DATA_KEY =
        Key.create("consulo.ide.impl.idea.ide.hierarchy.MethodHierarchyBrowserBase");

    public MethodHierarchyBrowserBase(Project project, PsiElement method) {
        super(project, method);
    }

    @Override
    @Nonnull
    protected String getPrevOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyMethodPrevOccurenceName().get();
    }

    @Override
    @Nonnull
    protected String getNextOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyMethodNextOccurenceName().get();
    }

    protected static JPanel createStandardLegendPanel(
        String methodDefinedText,
        String methodNotDefinedLegallyText,
        String methodShouldBeDefined
    ) {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gc =
            new GridBagConstraints(
                0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                JBUI.insets(3, 5, 0, 5),
                0,
                0
            );

        JLabel label = new JBLabel(methodDefinedText, PlatformIconGroup.hierarchyMethoddefined(), SwingConstants.LEFT);
        label.setUI(new MultiLineLabelUI());
        label.setIconTextGap(10);
        panel.add(label, gc);

        gc.gridy++;
        label = new JBLabel(methodNotDefinedLegallyText, PlatformIconGroup.hierarchyMethodnotdefined(), SwingConstants.LEFT);
        label.setUI(new MultiLineLabelUI());
        label.setIconTextGap(10);
        panel.add(label, gc);

        gc.gridy++;
        label = new JBLabel(methodShouldBeDefined, PlatformIconGroup.hierarchyShoulddefinemethod(), SwingConstants.LEFT);
        label.setUI(new MultiLineLabelUI());
        label.setIconTextGap(10);
        panel.add(label, gc);

        return panel;
    }

    @Override
    protected void prependActions(@Nonnull DefaultActionGroup actionGroup) {
        actionGroup.add(new AlphaSortAction());
        actionGroup.add(new ShowImplementationsOnlyAction());
    }

    @Override
    @Nonnull
    protected Key getBrowserDataKey() {
        return DATA_KEY;
    }

    @Override
    @Nonnull
    protected String getActionPlace() {
        return ActionPlaces.METHOD_HIERARCHY_VIEW_TOOLBAR;
    }

    private final class ShowImplementationsOnlyAction extends ToggleAction {
        private ShowImplementationsOnlyAction() {
            super(IdeLocalize.actionHideNonImplementations(), LocalizeValue.empty(), PlatformIconGroup.gutterImplementedmethod());
        }

        @Override
        public final boolean isSelected(@Nonnull AnActionEvent event) {
            return HierarchyBrowserManager.getInstance(myProject).getState().HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED;
        }

        @Override
        public final void setSelected(@Nonnull AnActionEvent event, boolean flag) {
            HierarchyBrowserManager.getInstance(myProject).getState().HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED = flag;

            // invokeLater is called to update state of button before long tree building operation
            Application.get().invokeLater(() -> doRefresh(true));
        }

        @Override
        @RequiredUIAccess
        public final void update(@Nonnull AnActionEvent event) {
            super.update(event);
            Presentation presentation = event.getPresentation();
            presentation.setEnabled(isValidBase());
        }
    }

    public static class BaseOnThisMethodAction extends BaseOnThisElementAction<MethodHierarchyProvider> {
        public BaseOnThisMethodAction() {
            super(
                IdeLocalize.actionBaseOnThisMethod(),
                IdeActions.ACTION_METHOD_HIERARCHY,
                DATA_KEY,
                MethodHierarchyProvider.class
            );
        }
    }
}
