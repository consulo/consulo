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
package consulo.compiler.artifact.element;

import consulo.compiler.artifact.ArtifactType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class CompositePackagingElement<S> extends PackagingElement<S> implements RenameablePackagingElement {
  private final List<PackagingElement<?>> myChildren = new ArrayList<PackagingElement<?>>();
  private List<PackagingElement<?>> myUnmodifiableChildren;

  protected CompositePackagingElement(PackagingElementType type) {
    super(type);
  }

  public <T extends PackagingElement<?>> T addOrFindChild(@Nonnull T child) {
    for (PackagingElement<?> element : myChildren) {
      if (element.isEqualTo(child)) {
        if (element instanceof CompositePackagingElement) {
          List<PackagingElement<?>> children = ((CompositePackagingElement<?>)child).getChildren();
          ((CompositePackagingElement<?>)element).addOrFindChildren(children);
        }
        //noinspection unchecked
        return (T)element;
      }
    }
    myChildren.add(child);
    return child;
  }

  public void addFirstChild(@Nonnull PackagingElement<?> child) {
    myChildren.add(0, child);
    for (int i = 1; i < myChildren.size(); i++) {
      PackagingElement<?> element = myChildren.get(i);
      if (element.isEqualTo(child)) {
        if (element instanceof CompositePackagingElement<?>) {
          ((CompositePackagingElement<?>)child).addOrFindChildren(((CompositePackagingElement<?>)element).getChildren());
        }
        myChildren.remove(i);
        break;
      }
    }
  }

  public List<? extends PackagingElement<?>> addOrFindChildren(Collection<? extends PackagingElement<?>> children) {
    List<PackagingElement<?>> added = new ArrayList<PackagingElement<?>>();
    for (PackagingElement<?> child : children) {
      added.add(addOrFindChild(child));
    }
    return added;
  }

  @Nullable
  public PackagingElement<?> moveChild(int index, int direction) {
    int target = index + direction;
    if (0 <= index && index < myChildren.size() && 0 <= target && target < myChildren.size()) {
      PackagingElement<?> element1 = myChildren.get(index);
      PackagingElement<?> element2 = myChildren.get(target);
      myChildren.set(index, element2);
      myChildren.set(target, element1);
      return element1;
    }
    return null;
  }

  public void removeChild(@Nonnull PackagingElement<?> child) {
    myChildren.remove(child);
  }

  public void removeChildren(@Nonnull Collection<? extends PackagingElement<?>> children) {
    myChildren.removeAll(children);
  }

  @Nonnull
  public List<PackagingElement<?>> getChildren() {
    if (myUnmodifiableChildren == null) {
      myUnmodifiableChildren = Collections.unmodifiableList(myChildren);
    }
    return myUnmodifiableChildren;
  }

  @Override
  public boolean canBeRenamed() {
    return true;
  }

  protected void computeChildrenInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                             @Nonnull PackagingElementResolvingContext resolvingContext,
                                             @Nonnull ArtifactIncrementalCompilerContext compilerContext,
                                             ArtifactType artifactType) {
    for (PackagingElement<?> child : myChildren) {
      child.computeIncrementalCompilerInstructions(creator, resolvingContext, compilerContext, artifactType);
    }
  }

  public void removeAllChildren() {
    myChildren.clear();
  }

  @Nullable
  public CompositePackagingElement<?> findCompositeChild(@Nonnull String name) {
    for (PackagingElement<?> child : myChildren) {
      if (child instanceof CompositePackagingElement && name.equals(((CompositePackagingElement)child).getName())) {
        return (CompositePackagingElement)child;
      }
    }
    return null;
  }
}
