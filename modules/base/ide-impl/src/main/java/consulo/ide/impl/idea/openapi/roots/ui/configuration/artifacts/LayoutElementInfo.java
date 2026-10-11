/*
 * Copyright 2013-2026 consulo.io
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
import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.ui.ex.tree.PresentationData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class LayoutElementInfo {
    private final List<PackagingElement<?>> myElements = new ArrayList<>();
    private final Map<PackagingElement<?>, @Nullable CompositePackagingElement<?>> myParentElements = new IdentityHashMap<>();
    private final Map<PackagingElement<?>, List<PackagingNodeSource>> myNodeSources = new IdentityHashMap<>();
    private final @Nullable PackagingElement<?> myParentValue;
    private final TreeNodePresentation myPresentation;
    private final PresentationData myPresentationData;

    private List<LayoutElementInfo> myChildren = List.of();
    private List<ArtifactProblem> myProblems = List.of();

    public LayoutElementInfo(
        PackagingElement<?> element,
        @Nullable PackagingElement<?> parentValue,
        @Nullable CompositePackagingElement<?> parentElement,
        Collection<PackagingNodeSource> nodeSources,
        TreeNodePresentation presentation
    ) {
        myParentValue = parentValue;
        myPresentation = presentation;
        myPresentationData = ArtifactTreePresentations.createPresentationData(presentation);
        addElement(element, parentElement, nodeSources);
    }

    public void addElement(PackagingElement<?> element, @Nullable CompositePackagingElement<?> parentElement, Collection<PackagingNodeSource> nodeSources) {
        myElements.add(element);
        myParentElements.put(element, parentElement);
        myNodeSources.computeIfAbsent(element, key -> new ArrayList<>()).addAll(nodeSources);
    }

    public PackagingElement<?> getFirstElement() {
        return myElements.get(0);
    }

    public List<PackagingElement<?>> getElements() {
        return myElements;
    }

    public @Nullable PackagingElement<?> getElementIfSingle() {
        return myElements.size() == 1 ? myElements.get(0) : null;
    }

    public @Nullable CompositePackagingElement<?> getParentElement(PackagingElement<?> element) {
        return myParentElements.get(element);
    }

    public @Nullable PackagingElement<?> getParentValue() {
        return myParentValue;
    }

    public boolean isRoot() {
        return myParentValue == null;
    }

    public List<PackagingNodeSource> getNodeSource(PackagingElement<?> element) {
        List<PackagingNodeSource> sources = myNodeSources.get(element);
        return sources == null ? List.of() : sources;
    }

    public List<PackagingNodeSource> getNodeSources() {
        List<PackagingNodeSource> sources = new ArrayList<>();
        for (List<PackagingNodeSource> elementSources : myNodeSources.values()) {
            sources.addAll(elementSources);
        }
        return sources;
    }

    public TreeNodePresentation getPresentation() {
        return myPresentation;
    }

    public PresentationData getPresentationData() {
        return myPresentationData;
    }

    public List<LayoutElementInfo> getChildren() {
        return myChildren;
    }

    public void setChildren(List<LayoutElementInfo> children) {
        myChildren = children;
    }

    public List<ArtifactProblem> getProblems() {
        return myProblems;
    }

    public void setProblems(List<ArtifactProblem> problems) {
        myProblems = problems;
    }
}
