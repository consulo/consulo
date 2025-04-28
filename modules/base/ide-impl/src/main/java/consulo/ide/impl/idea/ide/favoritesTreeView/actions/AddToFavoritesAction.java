/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.bookmark.ui.view.BookmarkNodeProvider;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.*;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author anna
 * @since 2005-02-15
 */
public class AddToFavoritesAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AddToFavoritesAction.class);
    private static final Set<String> POPUP_PLACES_IN_PROJECT_VIEW = Set.of(
        ActionPlaces.J2EE_VIEW_POPUP,
        ActionPlaces.STRUCTURE_VIEW_POPUP,
        ActionPlaces.PROJECT_VIEW_POPUP
    );

    private final String myFavoritesListName;

    public AddToFavoritesAction(String choosenList) {
        getTemplatePresentation().setText(choosenList, false);
        myFavoritesListName = choosenList;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        Collection<AbstractTreeNode> nodesToAdd = getNodesToAdd(dataContext, true);

        if (nodesToAdd != null && !nodesToAdd.isEmpty()) {
            Project project = e.getData(Project.KEY);
            FavoritesManagerImpl.getInstance(project).addRoots(myFavoritesListName, nodesToAdd);
        }
    }

    @RequiredReadAction
    public static Collection<AbstractTreeNode> getNodesToAdd(DataContext dataContext, boolean inProjectView) {
        Project project = dataContext.getData(Project.KEY);

        if (project == null) {
            return Collections.emptyList();
        }

        Module moduleContext = dataContext.getData(LangDataKeys.MODULE_CONTEXT);

        Collection<AbstractTreeNode> nodesToAdd = null;
        for (BookmarkNodeProvider provider : project.getExtensionList(BookmarkNodeProvider.class)) {
            nodesToAdd = provider.getFavoriteNodes(dataContext, ViewSettings.DEFAULT);
            if (nodesToAdd != null) {
                break;
            }
        }

        if (nodesToAdd == null) {
            Object elements = collectSelectedElements(dataContext);
            if (elements != null) {
                nodesToAdd = createNodes(project, moduleContext, elements, inProjectView, ViewSettings.DEFAULT);
            }
        }
        return nodesToAdd;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(canCreateNodes(e));
    }

    @RequiredReadAction
    public static boolean canCreateNodes(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        if (e.getData(Project.KEY) == null) {
            return false;
        }
        String place = e.getPlace();
        if (ActionPlaces.FAVORITES_VIEW_POPUP.equals(place)
            && dataContext.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY) == null) {
            return false;
        }
        boolean inProjectView = POPUP_PLACES_IN_PROJECT_VIEW.contains(place);
        //consulo.ide.impl.idea.openapi.actionSystem.ActionPlaces.USAGE_VIEW_TOOLBAR
        return getNodesToAdd(dataContext, inProjectView) != null;
    }

    static Object retrieveData(Object object, Object data) {
        return object == null ? data : object;
    }

    private static Object collectSelectedElements(DataContext dataContext) {
        Object elements = retrieveData(null, dataContext.getData(PsiElement.KEY));
        elements = retrieveData(elements, dataContext.getData(PsiElement.KEY_OF_ARRAY));
        elements = retrieveData(elements, dataContext.getData(PsiFile.KEY));
        elements = retrieveData(elements, dataContext.getData(ModuleGroup.ARRAY_DATA_KEY));
        elements = retrieveData(elements, dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY));
        elements = retrieveData(elements, dataContext.getData(LibraryGroupElement.ARRAY_DATA_KEY));
        elements = retrieveData(elements, dataContext.getData(NamedLibraryElement.ARRAY_DATA_KEY));
        elements = retrieveData(elements, dataContext.getData(VirtualFile.KEY));
        elements = retrieveData(elements, dataContext.getData(VirtualFile.KEY_OF_ARRAY));
        return elements;
    }

    @Nonnull
    @RequiredReadAction
    public static Collection<AbstractTreeNode> createNodes(
        Project project,
        Module moduleContext,
        Object object,
        boolean inProjectView,
        ViewSettings favoritesConfig
    ) {
        if (project == null) {
            return Collections.emptyList();
        }
        ArrayList<AbstractTreeNode> result = new ArrayList<>();
        for (BookmarkNodeProvider provider : project.getExtensionList(BookmarkNodeProvider.class)) {
            AbstractTreeNode treeNode = provider.createNode(project, object, favoritesConfig);
            if (treeNode != null) {
                result.add(treeNode);
                return result;
            }
        }
        PsiManager psiManager = PsiManager.getInstance(project);

        String currentViewId = ProjectView.getInstance(project).getCurrentViewId();
        ProjectViewPane pane = ProjectView.getInstance(project).getProjectViewPaneById(currentViewId);

        //on psi elements
        if (object instanceof PsiElement[]) {
            for (PsiElement psiElement : (PsiElement[])object) {
                addPsiElementNode(psiElement, project, result, favoritesConfig);
            }
            return result;
        }

        //on psi element
        if (object instanceof PsiElement element) {
            Module containingModule = null;
            if (inProjectView && ProjectView.getInstance(project).isShowModules(currentViewId)) {
                if (pane != null && pane.getSelectedDescriptor() != null
                    && pane.getSelectedDescriptor().getElement() instanceof AbstractTreeNode abstractTreeNode) {
                    while (abstractTreeNode != null && !(abstractTreeNode.getParent() instanceof AbstractModuleNode)) {
                        abstractTreeNode = (AbstractTreeNode)abstractTreeNode.getParent();
                    }
                    if (abstractTreeNode != null) {
                        containingModule = ((AbstractModuleNode)abstractTreeNode.getParent()).getValue();
                    }
                }
            }
            addPsiElementNode(element, project, result, favoritesConfig);
            return result;
        }

        if (object instanceof VirtualFile[] virtualFiles) {
            for (VirtualFile vFile : virtualFiles) {
                PsiElement element = psiManager.findFile(vFile);
                if (element == null) {
                    element = psiManager.findDirectory(vFile);
                }
                addPsiElementNode(element, project, result, favoritesConfig);
            }
            return result;
        }

        //on form in editor
        if (object instanceof VirtualFile vFile) {
            PsiFile psiFile = psiManager.findFile(vFile);
            addPsiElementNode(psiFile, project, result, favoritesConfig);
            return result;
        }

        //on module groups
        if (object instanceof ModuleGroup[] moduleGroups) {
            for (ModuleGroup moduleGroup : moduleGroups) {
                result.add(new ProjectViewModuleGroupNode(project, moduleGroup, favoritesConfig));
            }
            return result;
        }

        //on module nodes
        if (object instanceof Module module) {
            object = new Module[]{module};
        }
        if (object instanceof Module[] modules) {
            for (Module module1 : modules) {
                result.add(new ProjectViewModuleNode(project, module1, favoritesConfig));
            }
            return result;
        }

        //on library group node
        if (object instanceof LibraryGroupElement[] libraryGroupElements) {
            for (LibraryGroupElement libraryGroup : libraryGroupElements) {
                result.add(new LibraryGroupNode(project, libraryGroup, favoritesConfig));
            }
            return result;
        }

        //on named library node
        if (object instanceof NamedLibraryElement[] namedLibraryElements) {
            for (NamedLibraryElement namedLibrary : namedLibraryElements) {
                result.add(new NamedLibraryElementNode(project, namedLibrary, favoritesConfig));
            }
            return result;
        }
        return result;
    }

    private static void addPsiElementNode(
        PsiElement psiElement,
        Project project,
        ArrayList<AbstractTreeNode> result,
        ViewSettings favoritesConfig
    ) {
        Class<? extends AbstractTreeNode> klass = getPsiElementNodeClass(psiElement);
        if (klass == null) {
            psiElement = PsiTreeUtil.getParentOfType(psiElement, PsiFile.class);
            if (psiElement != null) {
                klass = PsiFileNode.class;
            }
        }

        Object value = psiElement;
        try {
            if (klass != null && value != null) {
                result.add(ProjectViewNode.createTreeNode(klass, project, value, favoritesConfig));
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    private static Class<? extends AbstractTreeNode> getPsiElementNodeClass(PsiElement psiElement) {
        Class<? extends AbstractTreeNode> klass = null;
        if (psiElement instanceof PsiFile) {
            klass = PsiFileNode.class;
        }
        else if (psiElement instanceof PsiDirectory) {
            klass = PsiDirectoryNode.class;
        }
        return klass;
    }
}
