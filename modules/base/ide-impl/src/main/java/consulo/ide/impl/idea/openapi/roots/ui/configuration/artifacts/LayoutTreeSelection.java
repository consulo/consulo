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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.ui.ex.awt.tree.SimpleNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class LayoutTreeSelection {
  private final List<PackagingElementNode<?>> mySelectedNodes = new ArrayList<PackagingElementNode<?>>();
  private final List<PackagingElement<?>> mySelectedElements = new ArrayList<PackagingElement<?>>();
  private final Map<PackagingElement<?>, PackagingElementNode<?>> myElement2Node = new HashMap<PackagingElement<?>, PackagingElementNode<?>>();
  private final Map<PackagingElementNode<?>, TreePath> myNode2Path = new HashMap<PackagingElementNode<?>, TreePath>();

  public LayoutTreeSelection(@Nonnull LayoutTree tree) {
    TreePath[] paths = tree.getSelectionPaths();
    if (paths == null) {
      return;
    }

    for (TreePath path : paths) {
      SimpleNode node = tree.getNodeFor(path);
      if (node instanceof PackagingElementNode) {
        PackagingElementNode<?> elementNode = (PackagingElementNode<?>)node;
        mySelectedNodes.add(elementNode);
        myNode2Path.put(elementNode, path);
        for (PackagingElement<?> element : elementNode.getPackagingElements()) {
          mySelectedElements.add(element);
          myElement2Node.put(element, elementNode);
        }
      }
    }
  }

  public List<PackagingElementNode<?>> getNodes() {
    return mySelectedNodes;
  }

  public List<PackagingElement<?>> getElements() {
    return mySelectedElements;
  }

  public PackagingElementNode<?> getNode(@Nonnull PackagingElement<?> element) {
    return myElement2Node.get(element);
  }

  public TreePath getPath(@Nonnull PackagingElementNode<?> node) {
    return myNode2Path.get(node);
  }

  @Nullable
  public CompositePackagingElement<?> getCommonParentElement() {
    CompositePackagingElement<?> commonParent = null;
    for (PackagingElementNode<?> selectedNode : mySelectedNodes) {
      PackagingElement<?> element = selectedNode.getElementIfSingle();
      if (element == null) return null;

      CompositePackagingElement<?> parentElement = selectedNode.getParentElement(element);
      if (parentElement == null || commonParent != null && !commonParent.equals(parentElement)) {
        return null;
      }
      commonParent = parentElement;
    }
    return commonParent;
  }

  @Nullable
  public PackagingElement<?> getElementIfSingle() {
    return mySelectedElements.size() == 1 ? mySelectedElements.get(0) : null;
  }

  @Nullable
  public PackagingElementNode<?> getNodeIfSingle() {
    return mySelectedNodes.size() == 1 ? mySelectedNodes.get(0) : null;
  }
}
