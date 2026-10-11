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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.content.ContentFolderTypeProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ContentEntryNewFolderAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ToggleFolderStateAction;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Eugene Zhuravlev
 * @since 2023-10-09
 */
public class ContentEntryTreeEditor {
    private final Project myProject;
    private final ModuleConfigurationState myState;
    private final ContentEntryTreeModel myModel = new ContentEntryTreeModel();
    private final DefaultActionGroup myEditingActionsGroup = new DefaultActionGroup();
    private final ContentEntryEditorListener myContentEntryEditorListener = new ContentEntryEditorListener() {
        @Override
        @RequiredUIAccess
        public void foldersChanged(ContentEntryEditor editor) {
            scheduleRefresh();
        }
    };

    private @Nullable Tree<VirtualFile> myTree;
    private @Nullable ActionToolbar myToolbar;
    private @Nullable ContentEntryEditor myContentEntryEditor;
    private boolean myRefreshScheduled;

    public ContentEntryTreeEditor(Project project, ModuleConfigurationState state) {
        myProject = project;
        myState = state;
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        ApplicationTreeExecutorFactory executorFactory = myProject.getApplication().getInstance(ApplicationTreeExecutorFactory.class);

        Tree<VirtualFile> tree = Tree.create(null, myModel, executorFactory.forBackgroundThreadWithReadAction(uiDisposable));
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> {
            VirtualFile file = node.getValue();
            return file == null ? "" : file.getName();
        });
        tree.addSelectListener(event -> updateToolbar());
        tree.addContextMenuListener(this::showContextMenu);
        myTree = tree;

        updateMarkActions();

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, myEditingActionsGroup, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(tree);
        myToolbar = toolbar;

        DockLayout toolbarLayout = DockLayout.create(Space.NONE);
        toolbarLayout.left(Label.create(ProjectLocalize.modulePathsMarkAsLabel()));
        toolbarLayout.center(toolbar.getUIComponent());
        toolbarLayout.paddingBuilder().leftSet(Space.MEDIUM).apply();

        DockLayout layout = DockLayout.create(Space.NONE);
        layout.top(toolbarLayout);
        layout.center(ScrollableLayout.create(tree));
        return layout;
    }

    @RequiredUIAccess
    public void updateMarkActions() {
        myEditingActionsGroup.removeAll();

        List<ContentFolderTypeProvider> providers = new ArrayList<>(ContentFoldersSupportUtil.getSupportedFolders(myState.getRootModel()));
        providers.sort(Comparator.comparingInt(ContentFolderTypeProvider::getWeight));
        for (ContentFolderTypeProvider provider : providers) {
            myEditingActionsGroup.add(new ToggleFolderStateAction(this, provider));
        }

        updateToolbar();
    }

    public @Nullable ContentEntryEditor getContentEntryEditor() {
        return myContentEntryEditor;
    }

    @RequiredUIAccess
    public void setContentEntryEditor(@Nullable ContentEntryEditor contentEntryEditor) {
        ContentEntryEditor oldEditor = myContentEntryEditor;
        if (oldEditor == contentEntryEditor) {
            return;
        }

        if (oldEditor != null) {
            oldEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
        }

        myContentEntryEditor = contentEntryEditor;
        if (contentEntryEditor != null) {
            contentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);
        }

        boolean sameEntry = oldEditor != null
            && contentEntryEditor != null
            && oldEditor.getContentEntry().equals(contentEntryEditor.getContentEntry());
        refresh(!sameEntry);
    }

    @RequiredUIAccess
    public List<VirtualFile> getSelectedFiles() {
        Tree<VirtualFile> tree = myTree;
        if (tree == null) {
            return List.of();
        }

        List<VirtualFile> files = new ArrayList<>();
        for (TreeNode<VirtualFile> node : tree.getSelectedNodes()) {
            VirtualFile file = node.getValue();
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    public @Nullable Tree<VirtualFile> getTree() {
        return myTree;
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    public void scheduleRefresh() {
        if (myRefreshScheduled) {
            return;
        }

        myRefreshScheduled = true;
        UIAccess.current().give(() -> {
            myRefreshScheduled = false;
            refresh(false);
        });
    }

    @RequiredUIAccess
    public CompletableFuture<?> refresh(boolean expandRoot) {
        Tree<VirtualFile> tree = myTree;
        if (tree == null) {
            return CompletableFuture.completedFuture(null);
        }

        ContentEntryEditor editor = myContentEntryEditor;
        myModel.setSnapshot(editor == null ? null : editor.createSnapshot(hasPackageSupport()));

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Object> result = new CompletableFuture<>();
        tree.refreshAll().whenCompleteAsync((refreshed, error) -> {
            if (expandRoot && myTree == tree) {
                expandRoot(tree, uiAccess);
            }
            updateToolbar();
            result.complete(null);
        }, uiAccess);
        return result;
    }

    @RequiredUIAccess
    public void refreshAndSelect(String url) {
        UIAccess uiAccess = UIAccess.current();
        refresh(false).whenCompleteAsync((refreshed, error) -> select(url), uiAccess);
    }

    @RequiredUIAccess
    private void expandRoot(Tree<VirtualFile> tree, UIAccess uiAccess) {
        TreeNode<VirtualFile> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return;
        }

        rootNode.findChild(file -> true).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree) {
                tree.expand(node);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    public CompletableFuture<?> select(String url) {
        Tree<VirtualFile> tree = myTree;
        TreeNode<VirtualFile> rootNode = tree == null ? null : tree.getRootNode();
        if (tree == null || rootNode == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Object> result = new CompletableFuture<>();
        descend(rootNode, url).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree) {
                tree.select(node);
            }
            result.complete(null);
        }, uiAccess);
        return result;
    }

    private CompletableFuture<@Nullable TreeNode<VirtualFile>> descend(TreeNode<VirtualFile> node, String url) {
        VirtualFile file = node.getValue();
        if (file != null && url.equals(file.getUrl())) {
            return CompletableFuture.completedFuture(node);
        }

        return node.findChild(child -> VirtualFileUtil.isEqualOrAncestor(child.getUrl(), url)).thenCompose(child -> {
            if (child == null) {
                return CompletableFuture.completedFuture(null);
            }
            return descend(child, url);
        });
    }

    @RequiredUIAccess
    private void updateToolbar() {
        ActionToolbar toolbar = myToolbar;
        if (toolbar != null) {
            toolbar.updateActionsAsync();
        }
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        Tree<VirtualFile> tree = myTree;
        if (tree == null || myContentEntryEditor == null) {
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(myEditingActionsGroup);
        group.addSeparator();
        group.add(new ContentEntryNewFolderAction(this));

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group.build());
        menu.setTargetComponent(tree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    private boolean hasPackageSupport() {
        for (ModuleExtension<?> moduleExtension : myState.getRootModel().getExtensions()) {
            boolean supported = myProject.getApplication()
                .getExtensionPoint(PsiPackageSupportProvider.class)
                .anyMatchSafe(provider -> provider.isSupported(moduleExtension));
            if (supported) {
                return true;
            }
        }
        return false;
    }
}
