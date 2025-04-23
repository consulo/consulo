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
import consulo.language.editor.hierarchy.CallHierarchyProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.nodep.function.Function;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public abstract class CallHierarchyBrowserBase extends HierarchyBrowserBaseEx {
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String CALLEE_TYPE = IdeBundle.message("title.hierarchy.callees.of");
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String CALLER_TYPE = IdeBundle.message("title.hierarchy.callers.of");

    private static final Key<Object> CALL_HIERARCHY_BROWSER_DATA_KEY =
        Key.create("consulo.ide.impl.idea.ide.hierarchy.CallHierarchyBrowserBase");

    public CallHierarchyBrowserBase(@Nonnull Project project, @Nonnull PsiElement method) {
        super(project, method);
    }

    @Nullable
    @Override
    protected JPanel createLegendPanel() {
        return null;
    }

    @Nonnull
    @Override
    protected Key<Object> getBrowserDataKey() {
        return CALL_HIERARCHY_BROWSER_DATA_KEY;
    }

    @Override
    protected void prependActions(@Nonnull DefaultActionGroup actionGroup) {
        actionGroup.add(new ChangeViewTypeActionBase(
            IdeLocalize.actionCallerMethodsHierarchy(),
            IdeLocalize.actionCallerMethodsHierarchy(),
            PlatformIconGroup.hierarchySupertypes(),
            CALLER_TYPE
        ));
        actionGroup.add(new ChangeViewTypeActionBase(
            IdeLocalize.actionCalleeMethodsHierarchy(),
            IdeLocalize.actionCalleeMethodsHierarchy(),
            PlatformIconGroup.hierarchySubtypes(),
            CALLEE_TYPE
        ));
        actionGroup.add(new AlphaSortAction());
        actionGroup.add(new ChangeScopeAction());
    }

    @Override
    @Nonnull
    protected String getActionPlace() {
        return ActionPlaces.CALL_HIERARCHY_VIEW_TOOLBAR;
    }

    @Nonnull
    @Override
    protected String getPrevOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyCallPrevOccurenceName().get();
    }

    @Nonnull
    @Override
    protected String getNextOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyCallNextOccurenceName().get();
    }

    private class ChangeViewTypeActionBase extends ToggleAction {
        private final String myTypeName;

        private ChangeViewTypeActionBase(
            @Nonnull LocalizeValue shortDescription,
            @Nonnull LocalizeValue longDescription,
            Image icon,
            String typeName
        ) {
            super(shortDescription, longDescription, icon);
            myTypeName = typeName;
        }

        @Override
        public final boolean isSelected(@Nonnull AnActionEvent event) {
            return myTypeName.equals(myCurrentViewType);
        }

        @Override
        public final void setSelected(@Nonnull AnActionEvent event, boolean flag) {
            if (flag) {
//              setWaitCursor();
                // invokeLater is called to update state of button before long tree building operation
                Application.get().invokeLater(() -> changeView(myTypeName));
            }
        }

        @Override
        @RequiredUIAccess
        public final void update(@Nonnull AnActionEvent event) {
            super.update(event);
            setEnabled(isValidBase());
        }
    }

    protected static class BaseOnThisMethodAction extends BaseOnThisElementAction<CallHierarchyProvider> {
        public BaseOnThisMethodAction() {
            super(
                IdeLocalize.actionBaseOnThisMethod(),
                IdeActions.ACTION_CALL_HIERARCHY,
                CALL_HIERARCHY_BROWSER_DATA_KEY,
                CallHierarchyProvider.class
            );
        }
    }
}
