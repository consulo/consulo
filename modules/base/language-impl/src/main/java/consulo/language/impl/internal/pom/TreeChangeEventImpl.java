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

package consulo.language.impl.internal.pom;

import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.pom.PomModelAspect;
import consulo.language.pom.event.PomChangeSet;
import consulo.language.pom.event.TreeChange;
import consulo.language.pom.event.TreeChangeEvent;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.collection.MultiMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author ik
 */
public class TreeChangeEventImpl implements TreeChangeEvent {
  private final Map<CompositeElement, TreeChangeImpl> myChangedElements = new LinkedHashMap<>();
  private final MultiMap<CompositeElement, TreeChangeImpl> myChangesByAllParents = MultiMap.createSet();
  private final PomModelAspect myAspect;
  private final FileElement myFileElement;

  public TreeChangeEventImpl(@Nonnull PomModelAspect aspect, @Nonnull FileElement treeElement) {
    myAspect = aspect;
    myFileElement = treeElement;
  }

  @Override
  @Nonnull
  public FileElement getRootElement() {
    return myFileElement;
  }

  @Override
  @Nonnull
  public ASTNode[] getChangedElements() {
    return myChangedElements.keySet().toArray(ASTNode.EMPTY_ARRAY);
  }

  @Override
  public TreeChange getChangesByElement(@Nonnull ASTNode element) {
    return myChangedElements.get((CompositeElement)element);
  }

  public void addElementaryChange(@Nonnull CompositeElement parent) {
    TreeChangeImpl existing = myChangedElements.get(parent);
    if (existing != null) {
      existing.clearCache();
    }
    else if (!integrateIntoExistingChanges(parent)) {
      mergeChange(new TreeChangeImpl(parent));
    }
  }

  private boolean integrateIntoExistingChanges(CompositeElement nextParent) {
    for (CompositeElement eachParent : JBIterable.generate(nextParent, TreeElement::getTreeParent)) {
      CompositeElement superParent = eachParent.getTreeParent();
      TreeChangeImpl superChange = myChangedElements.get(superParent);
      if (superChange != null) {
        superChange.markChildChanged(eachParent, 0);
        return true;
      }
    }
    return false;
  }

  private void mergeChange(TreeChangeImpl nextChange) {
    CompositeElement newParent = nextChange.getChangedParent();

    for (TreeChangeImpl descendant : new ArrayList<>(myChangesByAllParents.get(newParent))) {
      TreeElement ancestorChild = findAncestorChild(newParent, descendant);
      if (ancestorChild != null) {
        nextChange.markChildChanged(ancestorChild, descendant.getLengthDelta());
      }

      unregisterChange(descendant);
    }

    registerChange(nextChange);
  }

  private void registerChange(TreeChangeImpl nextChange) {
    myChangedElements.put(nextChange.getChangedParent(), nextChange);
    for (CompositeElement eachParent : nextChange.getSuperParents()) {
      myChangesByAllParents.putValue(eachParent, nextChange);
    }
  }

  private void unregisterChange(TreeChangeImpl change) {
    myChangedElements.remove(change.getChangedParent());
    for (CompositeElement superParent : change.getSuperParents()) {
      myChangesByAllParents.remove(superParent, change);
    }
  }

  /**
   * @return a direct child of {@code ancestor} which contains {@code change}
   */
  @Nullable
  private static TreeElement findAncestorChild(@Nonnull CompositeElement ancestor, @Nonnull TreeChangeImpl change) {
    List<CompositeElement> superParents = change.getSuperParents();
    int index = superParents.indexOf(ancestor);
    return index < 0 ? null : index == 0 ? change.getChangedParent() : superParents.get(index - 1);
  }

  @Override
  @Nonnull
  public PomModelAspect getAspect() {
    return myAspect;
  }

  @Override
  public void merge(@Nonnull PomChangeSet next) {
    for (TreeChangeImpl change : ((TreeChangeEventImpl)next).myChangedElements.values()) {
      TreeChangeImpl existing = myChangedElements.get(change.getChangedParent());
      if (existing != null) {
        existing.appendChanges(change);
      }
      else if (!integrateIntoExistingChanges(change.getChangedParent())) {
        mergeChange(change);
      }
    }
  }

  public void fireEvents() {
    Collection<TreeChangeImpl> changes = ContainerUtil.sorted(myChangedElements.values());
    for (TreeChangeImpl change : changes) {
      change.fireEvents((PsiFile)myFileElement.getPsi());
    }
  }

  @Override
  public void beforeNestedTransaction() {
    // compute changes and remember them, to prevent lazy computation to happen in another transaction
    // when more changes might have occurred but shouldn't count in this transaction
    for (TreeChangeImpl change : myChangedElements.values()) {
      change.getAffectedChildren();
    }
  }

  public String toString() {
    return new ArrayList<>(myChangedElements.values()).toString();
  }

}
