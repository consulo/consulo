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
package com.intellij.openapi.roots.ui.configuration.artifacts.nodes;

import com.intellij.packaging.elements.ComplexPackagingElement;
import com.intellij.packaging.elements.CompositePackagingElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author nik
 */
public class PackagingNodeSource {
  private final ComplexPackagingElement<?> mySourceElement;
  private final PackagingElementNode<?> mySourceParentNode;
  private final CompositePackagingElement<?> mySourceParentElement;
  private final Collection<PackagingNodeSource> myParentSources;

  public PackagingNodeSource(@Nonnull ComplexPackagingElement<?> sourceElement,
                             @Nonnull PackagingElementNode<?> sourceParentNode,
                             @Nonnull CompositePackagingElement<?> sourceParentElement,
                             @Nullable Collection<PackagingNodeSource> parentSources) {
    mySourceElement = sourceElement;
    mySourceParentNode = sourceParentNode;
    mySourceParentElement = sourceParentElement;
    myParentSources = parentSources;
  }

  @Nonnull
  public ComplexPackagingElement<?> getSourceElement() {
    return mySourceElement;
  }

  @Nonnull
  public PackagingElementNode<?> getSourceParentNode() {
    return mySourceParentNode;
  }

  @Nonnull
  public CompositePackagingElement<?> getSourceParentElement() {
    return mySourceParentElement;
  }

  public Collection<PackagingNodeSource> getParentSources() {
    return myParentSources;
  }

  public String getPresentableName() {
    return mySourceElement.createPresentation(mySourceParentNode.getContext()).getPresentableName();
  }
}
