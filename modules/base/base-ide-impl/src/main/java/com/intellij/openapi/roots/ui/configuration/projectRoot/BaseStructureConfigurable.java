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

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.MasterDetailsState;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.preferences.MasterDetailsConfigurable;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public abstract class BaseStructureConfigurable extends MasterDetailsComponent implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll {
  protected boolean myUiDisposed = true;

  private boolean myWasTreeInitialized;

  protected boolean myAutoScrollEnabled = true;

  protected BaseStructureConfigurable(MasterDetailsState state) {
    super(state);
  }

  protected BaseStructureConfigurable() {
  }

  @Override
  protected void initTree() {
    if (myWasTreeInitialized) return;
    myWasTreeInitialized = true;

    super.initTree();
    new TreeSpeedSearch(myTree, treePath -> ((MyNode)treePath.getLastPathComponent()).getDisplayName(), true);
    ToolTipManager.sharedInstance().registerComponent(myTree);
    myTree.setCellRenderer(new ProjectStructureElementRenderer(null));
  }

  @Override
  @RequiredUIAccess
  public void disposeUIResources() {
    if (myUiDisposed) return;

    super.disposeUIResources();

    myUiDisposed = true;

    myAutoScrollHandler.cancelAllRequests();

    ///myContext.getDaemonAnalyzer().clear();
  }

  public void checkCanApply() throws ConfigurationException {
  }

  protected void addCollapseExpandActions(final List<AnAction> result) {
    final TreeExpander expander = new TreeExpander() {
      @Override
      public void expandAll() {
        TreeUtil.expandAll(myTree);
      }

      @Override
      public boolean canExpand() {
        return true;
      }

      @Override
      public void collapseAll() {
        TreeUtil.collapseAll(myTree, 0);
      }

      @Override
      public boolean canCollapse() {
        return true;
      }
    };
    final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
    result.add(actionsManager.createExpandAllAction(expander, myTree));
    result.add(actionsManager.createCollapseAllAction(expander, myTree));
  }

  @Nullable
  public ProjectStructureElement getSelectedElement() {
    final TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath != null && selectionPath.getLastPathComponent() instanceof MyNode) {
      MyNode node = (MyNode)selectionPath.getLastPathComponent();
      final Configurable configurable = node.getConfigurable();
      if (configurable instanceof ProjectStructureElementConfigurable) {
        return ((ProjectStructureElementConfigurable)configurable).getProjectStructureElement();
      }
    }
    return null;
  }

  private class MyFindUsagesAction extends FindUsagesInProjectStructureActionBase {

    public MyFindUsagesAction(JComponent parentComponent) {
      super(parentComponent);
    }

    @Override
    protected boolean isEnabled() {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        final MyNode node = (MyNode)selectionPath.getLastPathComponent();
        return !node.isDisplayInBold();
      }
      else {
        return false;
      }
    }

    @Override
    protected ProjectStructureElement getSelectedElement() {
      return BaseStructureConfigurable.this.getSelectedElement();
    }

    @Override
    protected RelativePoint getPointToShowResults() {
      final int selectedRow = myTree.getSelectionRows()[0];
      final Rectangle rowBounds = myTree.getRowBounds(selectedRow);
      final Point location = rowBounds.getLocation();
      location.x += rowBounds.width;
      return new RelativePoint(myTree, location);
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    //myContext.reset();

    myUiDisposed = false;

    if (!myWasTreeInitialized) {
      initTree();
      myTree.setShowsRootHandles(false);
      loadTree();
    }
    else {
      super.disposeUIResources();
      myTree.setShowsRootHandles(false);
      loadTree();
    }
    //for (ProjectStructureElement element : getProjectStructureElements()) {
    //  myContext.getDaemonAnalyzer().queueUpdate(element);
    //}

    super.reset();
  }

  @Nonnull
  protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
    return Collections.emptyList();
  }

  protected abstract void loadTree();


  @Override
  @Nonnull
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    final ArrayList<AnAction> result = new ArrayList<AnAction>();
    AbstractAddGroup addAction = createAddAction();
    if (addAction != null) {
      result.add(addAction);
    }
    result.add(new MyRemoveAction());

    final List<? extends AnAction> copyActions = createCopyActions(fromPopup);
    result.addAll(copyActions);
    result.add(AnSeparator.getInstance());

    result.add(new MyFindUsagesAction(myTree));


    return result;
  }

  @Nonnull
  protected List<? extends AnAction> createCopyActions(boolean fromPopup) {
    return Collections.emptyList();
  }

  public void onStructureUnselected() {
  }

  public void onStructureSelected() {
  }

  @Nullable
  protected abstract AbstractAddGroup createAddAction();

  protected class MyRemoveAction extends MyDeleteAction {
    public MyRemoveAction() {
      super(objects -> {
        Object[] editableObjects = ContainerUtil.mapNotNull(objects, object -> {
          if (object instanceof MyNode) {
            final MasterDetailsConfigurable namedConfigurable = ((MyNode)object).getConfigurable();
            if (namedConfigurable != null) {
              return namedConfigurable.getEditableObject();
            }
          }
          return null;
        }, new Object[0]);
        return editableObjects.length == objects.length && canBeRemoved(editableObjects);
      });
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      final TreePath[] paths = myTree.getSelectionPaths();
      if (paths == null) return;

      final Set<TreePath> pathsToRemove = new HashSet<TreePath>();
      for (TreePath path : paths) {
        if (removeFromModel(path)) {
          pathsToRemove.add(path);
        }
      }
      removePaths(pathsToRemove.toArray(new TreePath[pathsToRemove.size()]));
    }

    private boolean removeFromModel(final TreePath selectionPath) {
      final Object last = selectionPath.getLastPathComponent();

      if (!(last instanceof MyNode)) return false;

      final MyNode node = (MyNode)last;
      final MasterDetailsConfigurable configurable = node.getConfigurable();
      if (configurable == null) return false;
      final Object editableObject = configurable.getEditableObject();

      return removeObject(editableObject);
    }
  }

  protected boolean canBeRemoved(final Object[] editableObjects) {
    for (Object editableObject : editableObjects) {
      if (!canObjectBeRemoved(editableObject)) return false;
    }
    return true;
  }

  private static boolean canObjectBeRemoved(Object editableObject) {
    if (editableObject instanceof Sdk || editableObject instanceof Module || editableObject instanceof Artifact) {
      return true;
    }
    if (editableObject instanceof Library) {
      final LibraryTable table = ((Library)editableObject).getTable();
      return table == null || table.isEditable();
    }
    return false;
  }

  protected boolean removeObject(final Object editableObject) {
    // todo keep only removeModule() and removeFacet() here because other removeXXX() are empty here and overridden in subclasses? Override removeObject() instead?
    if (editableObject instanceof Sdk) {
      removeSdk((Sdk)editableObject);
    }
    else if (editableObject instanceof Module) {
      if (!removeModule((Module)editableObject)) return false;
    }
    else if (editableObject instanceof Library) {
      if (!removeLibrary((Library)editableObject)) return false;
    }
    else if (editableObject instanceof Artifact) {
      removeArtifact((Artifact)editableObject);
    }
    return true;
  }

  protected void removeArtifact(Artifact artifact) {
  }


  protected boolean removeLibrary(Library library) {
    return false;
  }

  protected boolean removeModule(final Module module) {
    return true;
  }

  protected void removeSdk(final Sdk editableObject) {
  }

  protected abstract static class AbstractAddGroup extends ActionGroup implements ActionGroupWithPreselection {

    protected AbstractAddGroup(String text, Image icon) {
      super(text, true);

      final Presentation presentation = getTemplatePresentation();
      presentation.setIcon(icon);

      final Keymap active = KeymapManager.getInstance().getActiveKeymap();
      if (active != null) {
        final Shortcut[] shortcuts = active.getShortcuts("NewElement");
        setShortcutSet(new CustomShortcutSet(shortcuts));
      }
    }

    public AbstractAddGroup(String text) {
      this(text, IconUtil.getAddIcon());
    }

    @Override
    public ActionGroup getActionGroup() {
      return this;
    }

    @Override
    public int getDefaultIndex() {
      return 0;
    }
  }
}
