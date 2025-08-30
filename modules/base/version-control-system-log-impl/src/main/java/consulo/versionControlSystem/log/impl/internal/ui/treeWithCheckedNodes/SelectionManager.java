/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.treeWithCheckedNodes;

import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.internal.PlusMinus;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * see {@link SelectedState}
 *
 * @author irengrig
 * @since 2011-02-07
 */
public class SelectionManager {
  private final SelectedState<VirtualFile> myState;
  private final Function<DefaultMutableTreeNode, VirtualFile> myNodeConvertor;
  private PlusMinus<VirtualFile> mySelectionChangeListener;

  public SelectionManager(int selectedSize, int queueSize, Function<DefaultMutableTreeNode, VirtualFile> nodeConvertor) {
    myNodeConvertor = nodeConvertor;
    myState = new SelectedState<>(selectedSize, queueSize);
  }

  public void toggleSelection(DefaultMutableTreeNode node) {
    StateWorker stateWorker = new StateWorker(node, myNodeConvertor);
    VirtualFile vf = stateWorker.getVf();
    if (vf == null) return;

    TreeNodeState state = getStateImpl(stateWorker);
    if (TreeNodeState.HAVE_SELECTED_ABOVE.equals(state)) return;
    if (TreeNodeState.CLEAR.equals(state) && (! myState.canAddSelection())) return;

    HashSet<VirtualFile> old = new HashSet<>(myState.getSelected());

    TreeNodeState futureState =
            myState.putAndPass(vf, TreeNodeState.SELECTED.equals(state) ? TreeNodeState.CLEAR : TreeNodeState.SELECTED);

    // for those possibly duplicate nodes (i.e. when we have root for module and root for VCS root, each file is shown twice in a tree ->
    // clear all suspicious cached)
    if (! TreeNodeState.SELECTED.equals(futureState)) {
      myState.clearAllCachedMatching(virtualFile -> VirtualFileUtil.isAncestor(virtualFile, vf, false));
    }
    stateWorker.iterateParents(myState, (virtualFile, state1) -> {
      if (TreeNodeState.SELECTED.equals(futureState)) {
        myState.putAndPass(virtualFile, TreeNodeState.HAVE_SELECTED_BELOW);
      } else {
        myState.remove(virtualFile);
      }
      return true;
    });
    // todo vf, vf - what is correct?
    myState.clearAllCachedMatching(vf1 -> VirtualFileUtil.isAncestor(stateWorker.getVf(), vf1, false));
    for (VirtualFile selected : myState.getSelected()) {
      if (VirtualFileUtil.isAncestor(stateWorker.getVf(), selected, true)) {
        myState.remove(selected);
      }
    }
    Set<VirtualFile> selectedAfter = myState.getSelected();
    if (mySelectionChangeListener != null && ! old.equals(selectedAfter)) {
      Set<VirtualFile> removed = CollectionsDelta.notInSecond(old, selectedAfter);
      Set<VirtualFile> newlyAdded = CollectionsDelta.notInSecond(selectedAfter, old);
      if (newlyAdded != null) {
        for (VirtualFile file : newlyAdded) {
          if (mySelectionChangeListener != null) {
            mySelectionChangeListener.plus(file);
          }
        }
      }
      if (removed != null) {
        for (VirtualFile file : removed) {
          if (mySelectionChangeListener != null) {
            mySelectionChangeListener.minus(file);
          }
        }
      }
    }
  }

  public boolean canAddSelection() {
    return myState.canAddSelection();
  }

  public void setSelection(Collection<VirtualFile> files) {
    myState.setSelection(files);
    for (VirtualFile file : files) {
      if (mySelectionChangeListener != null) {
        mySelectionChangeListener.plus(file);
      }
    }
  }

  public TreeNodeState getState(DefaultMutableTreeNode node) {
    return getStateImpl(new StateWorker(node, myNodeConvertor));
  }

  private TreeNodeState getStateImpl(StateWorker stateWorker) {
    if (stateWorker.getVf() == null) return TreeNodeState.CLEAR;

    TreeNodeState stateSelf = myState.get(stateWorker.getVf());
    if (stateSelf != null) return stateSelf;

    Ref<TreeNodeState> result = new Ref<>();
    stateWorker.iterateParents(myState, (virtualFile, state) -> {
      if (state != null) {
        if (TreeNodeState.SELECTED.equals(state) || TreeNodeState.HAVE_SELECTED_ABOVE.equals(state)) {
          result.set(myState.putAndPass(stateWorker.getVf(), TreeNodeState.HAVE_SELECTED_ABOVE));
        }
        return false; // exit
      }
      return true;
    });

    if (! result.isNull()) return  result.get();

    for (VirtualFile selected : myState.getSelected()) {
      if (VirtualFileUtil.isAncestor(stateWorker.getVf(), selected, true)) {
        return myState.putAndPass(stateWorker.getVf(), TreeNodeState.HAVE_SELECTED_BELOW);
      }
    }
    return TreeNodeState.CLEAR;
  }

  public void removeSelection(VirtualFile elementAt) {
    myState.remove(elementAt);
    myState.clearAllCachedMatching(f -> VirtualFileUtil.isAncestor(f, elementAt, false) || VirtualFileUtil.isAncestor(elementAt, f, false));
    if (mySelectionChangeListener != null) {
      mySelectionChangeListener.minus(elementAt);
    }
  }

  private static class StateWorker {
    private final DefaultMutableTreeNode myNode;
    private final Function<DefaultMutableTreeNode, VirtualFile> myConvertor;
    private VirtualFile myVf;

    private StateWorker(DefaultMutableTreeNode node, Function<DefaultMutableTreeNode, VirtualFile> convertor) {
      myNode = node;
      myConvertor = convertor;
      myVf = myConvertor.apply(node);
    }

    public VirtualFile getVf() {
      return myVf;
    }

    public void iterateParents(SelectedState<VirtualFile> states, BiPredicate<VirtualFile, TreeNodeState> parentsProcessor) {
      DefaultMutableTreeNode current = (DefaultMutableTreeNode) myNode.getParent();
      // up cycle
      while (current != null) {
        VirtualFile file = myConvertor.apply(current);
        if (file == null) return;

        TreeNodeState state = states.get(file);
        if (! parentsProcessor.test(file, state)) return;
        current = (DefaultMutableTreeNode)current.getParent();
      }
    }
  }

  public void setSelectionChangeListener(@Nullable PlusMinus<VirtualFile> selectionChangeListener) {
    mySelectionChangeListener = selectionChangeListener;
  }
}