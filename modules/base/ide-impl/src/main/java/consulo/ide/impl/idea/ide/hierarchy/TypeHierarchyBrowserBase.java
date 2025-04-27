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

import consulo.language.editor.hierarchy.TypeHierarchyProvider;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.DeleteProvider;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.PopupHandler;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Map;

public abstract class TypeHierarchyBrowserBase extends HierarchyBrowserBaseEx {
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String TYPE_HIERARCHY_TYPE = IdeBundle.message("title.hierarchy.class");
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String SUBTYPES_HIERARCHY_TYPE = IdeBundle.message("title.hierarchy.subtypes");
    @SuppressWarnings("UnresolvedPropertyKey")
    public static final String SUPERTYPES_HIERARCHY_TYPE = IdeBundle.message("title.hierarchy.supertypes");

    private boolean myIsInterface;

    private final MyDeleteProvider myDeleteElementProvider = new MyDeleteProvider();

    public static final Key<TypeHierarchyBrowserBase> DATA_KEY = Key.create("consulo.ide.impl.idea.ide.hierarchy.TypeHierarchyBrowserBase");

    public TypeHierarchyBrowserBase(final Project project, final PsiElement element) {
        super(project, element);
    }

    protected abstract boolean isInterface(PsiElement psiElement);

    protected void createTreeAndSetupCommonActions(@Nonnull Map<String, JTree> trees, String typeHierarchyActionGroupName) {
        ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction(typeHierarchyActionGroupName);
        createTreeAndSetupCommonActions(trees, group);
    }

    protected void createTreeAndSetupCommonActions(@Nonnull Map<String, JTree> trees, ActionGroup group) {
        final BaseOnThisTypeAction baseOnThisTypeAction = createBaseOnThisAction();
        final JTree tree1 = createTree(true);
        PopupHandler.installPopupHandler(tree1, group, ActionPlaces.TYPE_HIERARCHY_VIEW_POPUP, ActionManager.getInstance());
        baseOnThisTypeAction
            .registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_TYPE_HIERARCHY).getShortcutSet(), tree1);
        trees.put(TYPE_HIERARCHY_TYPE, tree1);

        final JTree tree2 = createTree(true);
        PopupHandler.installPopupHandler(tree2, group, ActionPlaces.TYPE_HIERARCHY_VIEW_POPUP, ActionManager.getInstance());
        baseOnThisTypeAction
            .registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_TYPE_HIERARCHY).getShortcutSet(), tree2);
        trees.put(SUPERTYPES_HIERARCHY_TYPE, tree2);

        final JTree tree3 = createTree(true);
        PopupHandler.installPopupHandler(tree3, group, ActionPlaces.TYPE_HIERARCHY_VIEW_POPUP, ActionManager.getInstance());
        baseOnThisTypeAction
            .registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_TYPE_HIERARCHY).getShortcutSet(), tree3);
        trees.put(SUBTYPES_HIERARCHY_TYPE, tree3);
    }

    @Nonnull
    protected BaseOnThisTypeAction createBaseOnThisAction() {
        return new BaseOnThisTypeAction();
    }

    protected abstract boolean canBeDeleted(PsiElement psiElement);

    protected abstract String getQualifiedName(PsiElement psiElement);

    public boolean isInterface() {
        return myIsInterface;
    }

    @Override
    protected void setHierarchyBase(@Nonnull PsiElement element) {
        super.setHierarchyBase(element);
        myIsInterface = isInterface(element);
    }

    @Override
    protected void prependActions(final DefaultActionGroup actionGroup) {
        actionGroup.add(new ViewClassHierarchyAction());
        actionGroup.add(new ViewSupertypesHierarchyAction());
        actionGroup.add(new ViewSubtypesHierarchyAction());
        actionGroup.add(new AlphaSortAction());
    }

    @Override
    @Nonnull
    protected Key getBrowserDataKey() {
        return DATA_KEY;
    }

    @Override
    @Nonnull
    protected String getActionPlace() {
        return ActionPlaces.TYPE_HIERARCHY_VIEW_TOOLBAR;
    }

    @Override
    public final Object getData(@Nonnull Key<?> dataId) {
        if (DeleteProvider.KEY == dataId) {
            return myDeleteElementProvider;
        }
        return super.getData(dataId);
    }

    @Override
    @Nonnull
    protected String getPrevOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyTypePrevOccurenceName().get();
    }

    @Override
    @Nonnull
    protected String getNextOccurenceActionNameImpl() {
        return IdeLocalize.hierarchyTypeNextOccurenceName().get();
    }

    private final class MyDeleteProvider implements DeleteProvider {
        @Override
        public final void deleteElement(@Nonnull final DataContext dataContext) {
            final PsiElement aClass = getSelectedElement();
            if (!canBeDeleted(aClass)) {
                return;
            }
            LocalHistoryAction a =
                LocalHistory.getInstance().startAction(IdeLocalize.progressDeletingClass(getQualifiedName(aClass)).get());
            try {
                final PsiElement[] elements = {aClass};
                DeleteHandler.deletePsiElement(elements, myProject);
            }
            finally {
                a.finish();
            }
        }

        @Override
        public final boolean canDeleteElement(@Nonnull final DataContext dataContext) {
            final PsiElement aClass = getSelectedElement();
            if (!canBeDeleted(aClass)) {
                return false;
            }
            final PsiElement[] elements = {aClass};
            return DeleteHandler.shouldEnableDeleteAction(elements);
        }
    }

    protected static class BaseOnThisTypeAction extends BaseOnThisElementAction<TypeHierarchyProvider> {
        public BaseOnThisTypeAction() {
            super("", IdeActions.ACTION_TYPE_HIERARCHY, DATA_KEY, TypeHierarchyProvider.class);
        }

        @Override
        protected String correctViewType(HierarchyBrowserBaseEx browser, String viewType) {
            if (((TypeHierarchyBrowserBase)browser).myIsInterface && TYPE_HIERARCHY_TYPE.equals(viewType)) {
                return SUBTYPES_HIERARCHY_TYPE;
            }
            return viewType;
        }

        @Override
        protected String getNonDefaultText(@Nonnull HierarchyBrowserBaseEx browser, @Nonnull PsiElement element) {
            return ((TypeHierarchyBrowserBase)browser).isInterface(element)
                ? IdeLocalize.actionBaseOnThisInterface().get()
                : IdeLocalize.actionBaseOnThisClass().get();
        }
    }
}
