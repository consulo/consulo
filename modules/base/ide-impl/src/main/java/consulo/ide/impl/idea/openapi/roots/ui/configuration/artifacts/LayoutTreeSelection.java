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

import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class LayoutTreeSelection {
    private final List<LayoutElementInfo> mySelectedNodes;
    private final List<PackagingElement<?>> mySelectedElements = new ArrayList<>();
    private final Map<PackagingElement<?>, LayoutElementInfo> myElement2Node = new IdentityHashMap<>();

    public LayoutTreeSelection(List<LayoutElementInfo> selectedNodes) {
        mySelectedNodes = List.copyOf(selectedNodes);
        for (LayoutElementInfo node : mySelectedNodes) {
            for (PackagingElement<?> element : node.getElements()) {
                mySelectedElements.add(element);
                myElement2Node.put(element, node);
            }
        }
    }

    public List<LayoutElementInfo> getNodes() {
        return mySelectedNodes;
    }

    public List<PackagingElement<?>> getElements() {
        return mySelectedElements;
    }

    public @Nullable LayoutElementInfo getNode(PackagingElement<?> element) {
        return myElement2Node.get(element);
    }

    public @Nullable CompositePackagingElement<?> getCommonParentElement() {
        CompositePackagingElement<?> commonParent = null;
        for (LayoutElementInfo selectedNode : mySelectedNodes) {
            PackagingElement<?> element = selectedNode.getElementIfSingle();
            if (element == null) {
                return null;
            }

            CompositePackagingElement<?> parentElement = selectedNode.getParentElement(element);
            if (parentElement == null || commonParent != null && !commonParent.equals(parentElement)) {
                return null;
            }
            commonParent = parentElement;
        }
        return commonParent;
    }

    public @Nullable PackagingElement<?> getElementIfSingle() {
        return mySelectedElements.size() == 1 ? mySelectedElements.get(0) : null;
    }

    public @Nullable LayoutElementInfo getNodeIfSingle() {
        return mySelectedNodes.size() == 1 ? mySelectedNodes.get(0) : null;
    }
}
