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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ComplexElementSubstitutionParameters;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ui.ex.awt.tree.SimpleNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author nik
 */
public class CompositePackagingElementNode extends PackagingElementNode<CompositePackagingElement<?>> {
  private final ComplexElementSubstitutionParameters mySubstitutionParameters;
  private final ArtifactType myArtifactType;

  public CompositePackagingElementNode(CompositePackagingElement<?> packagingElement, ArtifactEditorContext context,
                                       CompositePackagingElementNode parentNode, CompositePackagingElement<?> parentElement,
                                       ComplexElementSubstitutionParameters substitutionParameters,
                                       Collection<PackagingNodeSource> nodeSources, ArtifactType artifactType) {
    super(packagingElement, context, parentNode, parentElement, nodeSources);
    mySubstitutionParameters = substitutionParameters;
    myArtifactType = artifactType;
  }

  @Override
  protected SimpleNode[] buildChildren() {
    List<PackagingElementNode<?>> children = new ArrayList<PackagingElementNode<?>>();
    for (CompositePackagingElement<?> element : getPackagingElements()) {
      PackagingTreeNodeFactory.addNodes(element.getChildren(), this, element, myContext, mySubstitutionParameters, getNodeSource(element), children,
                                        myArtifactType, new HashSet<PackagingElement<?>>());
    }
    return children.isEmpty() ? NO_CHILDREN : children.toArray(new SimpleNode[children.size()]);
  }

  @Override
  protected void onChildrenBuilt() {
    ((ArtifactEditorImpl)myContext.getThisArtifactEditor()).getValidationManager().onNodesAdded();
  }
}
