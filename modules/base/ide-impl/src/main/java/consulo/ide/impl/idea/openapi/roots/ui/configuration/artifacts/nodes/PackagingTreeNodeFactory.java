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
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.ComplexPackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class PackagingTreeNodeFactory {
  private PackagingTreeNodeFactory() {
  }

  public static void addNodes(@Nonnull List<? extends PackagingElement<?>> elements, @Nonnull CompositePackagingElementNode parentNode,
                              @Nonnull CompositePackagingElement parentElement, @Nonnull ArtifactEditorContext context,
                              @Nonnull ComplexElementSubstitutionParameters substitutionParameters, @Nonnull Collection<PackagingNodeSource> nodeSources,
                              @Nonnull List<PackagingElementNode<?>> nodes,
                              ArtifactType artifactType,
                              Set<PackagingElement<?>> processed) {
    for (PackagingElement<?> element : elements) {
      PackagingElementNode<?> prev = findEqual(nodes, element);
      if (prev != null) {
        prev.addElement(element, parentElement, nodeSources);
        continue;
      }

      if (element instanceof ArtifactRootElement) {
        throw new AssertionError("artifact root not expected here");
      }
      else if (element instanceof CompositePackagingElement) {
        nodes.add(new CompositePackagingElementNode((CompositePackagingElement<?>)element, context, parentNode, parentElement, substitutionParameters, nodeSources,
                                                    artifactType));
      }
      else if (element instanceof ComplexPackagingElement) {
        ComplexPackagingElement<?> complexElement = (ComplexPackagingElement<?>)element;
        if (processed.add(element) && substitutionParameters.shouldSubstitute(complexElement)) {
          List<? extends PackagingElement<?>> substitution = complexElement.getSubstitution(context, artifactType);
          if (substitution != null) {
            PackagingNodeSource source = new PackagingNodeSource(complexElement, parentNode, parentElement, nodeSources);
            addNodes(substitution, parentNode, parentElement, context, substitutionParameters, Collections.singletonList(source), nodes,
                     artifactType, processed);
            continue;
          }
        }
        nodes.add(new ComplexPackagingElementNode(complexElement, context, parentNode, parentElement, substitutionParameters, nodeSources));
      }
      else {
        nodes.add(new PackagingElementNode<PackagingElement<?>>(element, context, parentNode, parentElement, nodeSources));
      }
    }
  }

  @Nullable
  private static PackagingElementNode<?> findEqual(List<PackagingElementNode<?>> children, PackagingElement<?> element) {
    for (PackagingElementNode<?> node : children) {
      if (node.getFirstElement().isEqualTo(element)) {
        return node;
      }
    }
    return null;
  }

  @Nonnull
  public static ArtifactRootNode createRootNode(ArtifactEditorImpl artifactsEditor, ArtifactEditorContext context,
                                                ComplexElementSubstitutionParameters substitutionParameters, ArtifactType artifactType) {
    return new ArtifactRootNode(artifactsEditor, context, substitutionParameters, artifactType);
  }
}
