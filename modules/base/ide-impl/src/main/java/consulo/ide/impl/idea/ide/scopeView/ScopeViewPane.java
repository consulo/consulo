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

package consulo.ide.impl.idea.ide.scopeView;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.projectView.ProjectView;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.ShowModulesAction;
import consulo.ide.impl.idea.packageDependencies.DependencyValidationManager;
import consulo.ide.impl.idea.packageDependencies.ui.PackageDependenciesNode;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.view.SelectInTarget;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.concurrent.EdtExecutorService;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author cdr
 */
@ExtensionImpl
public class ScopeViewPane extends AbstractProjectViewPane {
  private static final Logger LOG = Logger.getInstance(ScopeViewPane.class);
  private LinkedHashMap<String, NamedScopeFilter> myFilters;

  @NonNls
  public static final String ID = "Scope";
  private final ProjectView myProjectView;
  private ScopeTreeViewPanel myViewPanel;
  private final DependencyValidationManager myDependencyValidationManager;
  private final NamedScopeManager myNamedScopeManager;

  @Inject
  public ScopeViewPane(final Project project, ProjectView projectView, DependencyValidationManager dependencyValidationManager, NamedScopeManager namedScopeManager) {
    super(project);
    myProjectView = projectView;
    myDependencyValidationManager = dependencyValidationManager;
    myNamedScopeManager = namedScopeManager;
    myFilters = map(myDependencyValidationManager, myNamedScopeManager);

    NamedScopesHolder.ScopeListener scopeListener = new NamedScopesHolder.ScopeListener() {
      private final AtomicLong counter = new AtomicLong();

      @Override
      public void scopesChanged() {
        if (myProject.isDisposed()) {
          return;
        }

        long count = counter.incrementAndGet();
        EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
          // is this request still actual after 10 ms?
          if (count != counter.get()) {
            return;
          }

          ProjectView view = myProject.isDisposed() ? null : ProjectView.getInstance(myProject);
          if (view == null) {
            return;
          }
          myFilters = map(myDependencyValidationManager, myNamedScopeManager);
          String currentId = view.getCurrentViewId();
          String currentSubId = getSubId();
          // update changes subIds if needed
          view.removeProjectPane(ScopeViewPane.this);
          view.addProjectPane(ScopeViewPane.this);
          if (currentId == null) {
            return;
          }
          if (currentId.equals(getId())) {
            // try to restore selected subId
            view.changeView(currentId, currentSubId);
          }
          else {
            view.changeView(currentId);
          }
        }, 10, TimeUnit.MILLISECONDS);
      }
    };

    myDependencyValidationManager.addScopeListener(scopeListener, this);
    myNamedScopeManager.addScopeListener(scopeListener, this);
  }

  @Nonnull
  @Override
  public String getTitle() {
    return IdeBundle.message("scope.view.title");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Ide.LocalScope;
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }

  @Nonnull
  @Override
  public JComponent createComponent() {
    myViewPanel = new ScopeTreeViewPanel(myProject);
    Disposer.register(this, myViewPanel);
    myViewPanel.initListeners();
    myViewPanel.selectScope(NamedScopesHolder.getScope(myProject, getSubId()));
    myTree = myViewPanel.getTree();
    PopupHandler.installPopupHandler(myTree, IdeActions.GROUP_SCOPE_VIEW_POPUP, ActionPlaces.SCOPE_VIEW_POPUP);
    enableDnD();

    return myViewPanel.getPanel();
  }

  @Override
  public void dispose() {
    myViewPanel = null;
    super.dispose();
  }

  @Override
  @Nonnull
  public String [] getSubIds() {
    LinkedHashMap<String, NamedScopeFilter> map = myFilters;
    if (map == null || map.isEmpty()) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }
    return ArrayUtil.toStringArray(map.keySet());
  }

  @Nonnull
  @Override
  public String getPresentableSubIdName(@Nonnull String subId) {
    NamedScopeFilter filter = getFilter(subId);
    return filter == null ? getTitle() : filter.getScope().getPresentableName().getValue();
  }

  @Nonnull
  @Override
  public Image getPresentableSubIdIcon(@Nonnull String subId) {
    NamedScopeFilter filter = getFilter(subId);
    if (filter != null) {
      NamedScope scope = filter.getScope();
      return scope.getIconForProjectView();
    }
    else {
      return getIcon();
    }
  }

  @Override
  public void addToolbarActions(DefaultActionGroup actionGroup) {
    actionGroup.add(ActionManager.getInstance().getAction("ScopeView.EditScopes"));
    actionGroup.addAction(new ShowModulesAction(myProject) {
      @Override
      protected String getId() {
        return ScopeViewPane.this.getId();
      }
    }).setAsSecondary(true);
  }

  @Nonnull
  @Override
  public ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
    saveExpandedPaths();
    myViewPanel.selectScope(NamedScopesHolder.getScope(myProject, getSubId()));
    restoreExpandedPaths();
    return new ActionCallback.Done();
  }

  @Override
  public void select(Object element, VirtualFile file, boolean requestFocus) {
    if (file == null) return;
    PsiFileSystemItem psiFile = file.isDirectory() ? PsiManager.getInstance(myProject).findDirectory(file) : PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) return;
    if (!(element instanceof PsiElement)) return;

    List<NamedScope> allScopes = new ArrayList<NamedScope>();
    ContainerUtil.addAll(allScopes, myDependencyValidationManager.getScopes());
    ContainerUtil.addAll(allScopes, myNamedScopeManager.getScopes());
    for (int i = 0; i < allScopes.size(); i++) {
      final NamedScope scope = allScopes.get(i);
      String name = scope.getName();
      if (name.equals(getSubId())) {
        allScopes.set(i, allScopes.get(0));
        allScopes.set(0, scope);
        break;
      }
    }
    for (NamedScope scope : allScopes) {
      String name = scope.getName();
      PackageSet packageSet = scope.getValue();
      if (packageSet == null) continue;
      if (changeView(packageSet, ((PsiElement)element), psiFile, name, myNamedScopeManager, requestFocus)) break;
      if (changeView(packageSet, ((PsiElement)element), psiFile, name, myDependencyValidationManager, requestFocus)) break;
    }
  }

  private boolean changeView(final PackageSet packageSet,
                             final PsiElement element,
                             final PsiFileSystemItem psiFileSystemItem,
                             final String name,
                             final NamedScopesHolder holder,
                             boolean requestFocus) {
    if (packageSet.contains(psiFileSystemItem.getVirtualFile(), myProject, holder)) {
      if (!name.equals(getSubId())) {
        myProjectView.changeView(getId(), name);
      }
      myViewPanel.selectNode(element, psiFileSystemItem, requestFocus);
      return true;
    }
    return false;
  }


  @Override
  public int getWeight() {
    return 3;
  }

  @Override
  public void installComparator() {
    myViewPanel.setSortByType();
  }

  @Nonnull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new ScopePaneSelectInTarget(myProject);
  }

  @Override
  protected Object exhumeElementFromNode(final DefaultMutableTreeNode node) {
    if (node instanceof PackageDependenciesNode) {
      return ((PackageDependenciesNode)node).getPsiElement();
    }
    return super.exhumeElementFromNode(node);
  }

  @Override
  public Object getData(@Nonnull final Key<?> dataId) {
    final Object data = super.getData(dataId);
    if (data != null) {
      return data;
    }
    return myViewPanel != null ? myViewPanel.getData(dataId) : null;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> getReady(@Nonnull Object requestor) {
    final AsyncResult<Void> callback = myViewPanel.getActionCallback();
    return myViewPanel == null ? AsyncResult.rejected() : callback != null ? callback : AsyncResult.resolved();
  }

  @Nullable
  public NamedScope getSelectedScope() {
    NamedScopeFilter filter = getFilter(getSubId());
    return filter == null ? null : filter.getScope();
  }

  @Nonnull
  Iterable<NamedScopeFilter> getFilters() {
    return myFilters.values();
  }

  @Nullable
  NamedScopeFilter getFilter(@Nullable String subId) {
    LinkedHashMap<String, NamedScopeFilter> map = myFilters;
    return map == null || subId == null ? null : map.get(subId);
  }

  @Nonnull
  private static LinkedHashMap<String, NamedScopeFilter> map(NamedScopesHolder... holders) {
    LinkedHashMap<String, NamedScopeFilter> map = new LinkedHashMap<>();
    for (NamedScopeFilter filter : NamedScopeFilter.list(holders)) {
      NamedScopeFilter old = map.put(filter.toString(), filter);
      if (old != null) {
        LOG.warn("DUPLICATED: " + filter);
      }
    }
    return map;
  }
}
