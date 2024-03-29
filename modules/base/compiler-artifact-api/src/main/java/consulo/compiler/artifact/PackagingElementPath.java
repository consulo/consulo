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
package consulo.compiler.artifact;

import consulo.compiler.artifact.element.*;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
public class PackagingElementPath {
  public static final PackagingElementPath EMPTY = new PackagingElementPath(null, null);
  private final PackagingElementPath myParentPath;
  private final PackagingElement<?> myLastElement;

  private PackagingElementPath(PackagingElementPath parentPath, PackagingElement<?> lastElement) {
    myParentPath = parentPath;
    myLastElement = lastElement;
  }

  public PackagingElementPath appendComplex(ComplexPackagingElement<?> element) {
    return new PackagingElementPath(this, element);
  }

  public PackagingElementPath appendComposite(CompositePackagingElement<?> element) {
    return new PackagingElementPath(this, element);
  }

  @Nonnull
  public String getPathString() {
    return getPathString("/");
  }

  @Nonnull
  public String getPathString(String separator) {
    return getPathStringFrom(separator, null);
  }

  @Nonnull
  public String getPathStringFrom(String separator, @Nullable CompositePackagingElement<?> ancestor) {
    final StringBuilder builder = new StringBuilder();
    final List<CompositePackagingElement<?>> parents = getParentsFrom(ancestor);
    for (int i = parents.size() - 1; i >= 0; i--) {
      builder.append(parents.get(i).getName());
      if (i > 0) {
        builder.append(separator);
      }
    }
    return builder.toString();
  }
  
  public List<CompositePackagingElement<?>> getParents() {
    return getParentsFrom(null);
  }

  public List<CompositePackagingElement<?>> getParentsFrom(@Nullable CompositePackagingElement<?> ancestor) {
    List<CompositePackagingElement<?>> result = new SmartList<CompositePackagingElement<?>>();
    PackagingElementPath path = this;
    while (path != EMPTY && path.myLastElement != ancestor) {
      if (path.myLastElement instanceof CompositePackagingElement<?>) {
        result.add((CompositePackagingElement)path.myLastElement);
      }
      path = path.myParentPath;
    }
    return result;
  }

  public List<PackagingElement<?>> getAllElements() {
    List<PackagingElement<?>> result = new SmartList<PackagingElement<?>>();
    PackagingElementPath path = this;
    while (path != EMPTY) {
      result.add(path.myLastElement);
      path = path.myParentPath;
    }
    return result;
  }

  @Nullable
  public CompositePackagingElement<?> getLastParent() {
    PackagingElementPath path = this;
    while (path != EMPTY) {
      if (path.myLastElement instanceof CompositePackagingElement<?>) {
        return (CompositePackagingElement)path.myLastElement;
      }
      path = path.myParentPath;
    }
    return null;
  }

  @Nullable
  public Artifact findLastArtifact(PackagingElementResolvingContext context) {
    PackagingElementPath path = this;
    while (path != EMPTY) {
      final PackagingElement<?> element = path.myLastElement;
      if (element instanceof ArtifactPackagingElement) {
        return ((ArtifactPackagingElement)element).findArtifact(context);
      }
      path = path.myParentPath;
    }
    return null;
  }

  public static PackagingElementPath createPath(@Nonnull List<PackagingElement<?>> elements) {
    PackagingElementPath path = EMPTY;
    for (PackagingElement<?> element : elements) {
      path = new PackagingElementPath(path, element);
    }
    return path;
  }
}
