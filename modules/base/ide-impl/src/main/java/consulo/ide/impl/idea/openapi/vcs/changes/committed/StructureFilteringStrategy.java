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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.ui.ex.awt.tree.TreeState;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserNode;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.TreeModelBuilder;
import consulo.versionControlSystem.change.commited.ChangeListFilteringStrategy;
import consulo.versionControlSystem.change.commited.CommittedChangesFilterKey;
import consulo.versionControlSystem.change.commited.CommittedChangesFilterPriority;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yole
 */
public class StructureFilteringStrategy implements ChangeListFilteringStrategy {
  private final List<ChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private MyUI myUI;
  private final Project myProject;
  private final List<FilePath> mySelection = new ArrayList<>();

  public StructureFilteringStrategy(final Project project) {
    myProject = project;
  }

  @Override
  public CommittedChangesFilterKey getKey() {
    return new CommittedChangesFilterKey(toString(), CommittedChangesFilterPriority.STRUCTURE);
  }

  public String toString() {
    return VcsLocalize.filterStructureName().get();
  }

  @jakarta.annotation.Nullable
  public JComponent getFilterUI() {
    if (myUI == null) {
      myUI = new MyUI();
    }
    return myUI.getComponent();
  }

  public void setFilterBase(List<CommittedChangeList> changeLists) {
    // todo cycle here
    if (myUI == null) {
      myUI = new MyUI();
    }
    myUI.reset();
    myUI.append(changeLists);
  }

  public void addChangeListener(ChangeListener listener) {
    myListeners.add(listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    myListeners.remove(listener);
  }

  public void resetFilterBase() {
    myUI.reset();
  }

  public void appendFilterBase(List<CommittedChangeList> changeLists) {
    myUI.append(changeLists);
  }

  @Nonnull
  public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
    if (mySelection.size() == 0) {
      return changeLists;
    }
    final ArrayList<CommittedChangeList> result = new ArrayList<>();
    for (CommittedChangeList list : changeLists) {
      if (listMatchesSelection(list)) {
        result.add(list);
      }
    }
    return result;
  }

  private boolean listMatchesSelection(final CommittedChangeList list) {
    for (Change change : list.getChanges()) {
      FilePath path = ChangesUtil.getFilePath(change);
      for (FilePath selPath : mySelection) {
        if (path.isUnder(selPath, false)) {
          return true;
        }
      }
    }
    return false;
  }

  private class MyUI {
    private final JComponent myScrollPane;
    private final Tree myStructureTree;
    private boolean myRendererInitialized;
    private final Set<FilePath> myFilePaths = new HashSet<>();
    private TreeState myState;

    public MyUI() {
      myStructureTree = new Tree();
      myStructureTree.setRootVisible(false);
      myStructureTree.setShowsRootHandles(true);
      myStructureTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
        public void valueChanged(final TreeSelectionEvent e) {
          final List<FilePath> filePaths = new ArrayList<>(mySelection);

          mySelection.clear();
          final TreePath[] selectionPaths = myStructureTree.getSelectionPaths();
          if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
              mySelection.addAll(getFilePathsUnder((ChangesBrowserNode<?>)selectionPath.getLastPathComponent()));
            }
          }

          if (Comparing.haveEqualElements(filePaths, mySelection)) return;

          for (ChangeListener listener : myListeners) {
            listener.stateChanged(new ChangeEvent(this));
          }
        }
      });
      myScrollPane = ScrollPaneFactory.createScrollPane(myStructureTree);
    }

    @Nonnull
    private List<FilePath> getFilePathsUnder(@Nonnull ChangesBrowserNode<?> node) {
      List<FilePath> result = Collections.emptyList();
      Object userObject = node.getUserObject();

      if (userObject instanceof FilePath) {
        result = ContainerUtil.list(((FilePath)userObject));
      }
      else if (userObject instanceof Module) {
        result = Arrays.stream(ModuleRootManager.getInstance((Module)userObject).getContentRoots())
                .map(VcsUtil::getFilePath)
                .collect(Collectors.toList());
      }

      return result;
    }

    public void initRenderer() {
      if (!myRendererInitialized) {
        myRendererInitialized = true;
        myStructureTree.setCellRenderer(new ChangesBrowserNodeRenderer(myProject, BooleanGetter.FALSE, false));
      }
    }

    public JComponent getComponent() {
      return myScrollPane;
    }

    public void reset() {
      myFilePaths.clear();
      myState = TreeState.createOn(myStructureTree, (DefaultMutableTreeNode)myStructureTree.getModel().getRoot());
      myStructureTree.setModel(TreeModelBuilder.buildEmpty(myProject));
    }

    public void append(final List<CommittedChangeList> changeLists) {
      final TreeState localState = myState != null && myFilePaths.isEmpty()
                                   ? myState
                                   : TreeState.createOn(myStructureTree, (DefaultMutableTreeNode)myStructureTree.getModel().getRoot());

      for (CommittedChangeList changeList : changeLists) {
        for (Change change : changeList.getChanges()) {
          final FilePath path = ChangesUtil.getFilePath(change);
          if (path.getParentPath() != null) {
            myFilePaths.add(path.getParentPath());
          }
        }
      }

      myStructureTree.setModel(TreeModelBuilder.buildFromFilePaths(myProject, false, myFilePaths));
      localState.applyTo(myStructureTree, (DefaultMutableTreeNode)myStructureTree.getModel().getRoot());
      myStructureTree.revalidate();
      myStructureTree.repaint();
      initRenderer();
    }
  }
}
