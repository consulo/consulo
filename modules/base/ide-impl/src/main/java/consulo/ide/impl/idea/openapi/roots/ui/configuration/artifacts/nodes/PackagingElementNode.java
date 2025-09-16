/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactProblemDescription;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemType;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.MultiValuesMap;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class PackagingElementNode<E extends PackagingElement<?>> extends ArtifactsTreeNode {
    private final List<E> myPackagingElements;
    private final Map<PackagingElement<?>, CompositePackagingElement<?>> myParentElements = new HashMap<>(1);
    private final MultiValuesMap<PackagingElement<?>, PackagingNodeSource> myNodeSources = new MultiValuesMap<>();
    private final CompositePackagingElementNode myParentNode;

    public PackagingElementNode(
        @Nonnull E packagingElement, ArtifactEditorContext context, @Nullable CompositePackagingElementNode parentNode,
        @Nullable CompositePackagingElement<?> parentElement,
        @Nonnull Collection<PackagingNodeSource> nodeSources
    ) {
        super(context, parentNode, packagingElement.createPresentation(context));
        myParentNode = parentNode;
        myParentElements.put(packagingElement, parentElement);
        myNodeSources.putAll(packagingElement, nodeSources);
        myPackagingElements = new SmartList<>();
        doAddElement(packagingElement);
    }

    private void doAddElement(E packagingElement) {
        myPackagingElements.add(packagingElement);
    }

    @Nullable
    public CompositePackagingElement<?> getParentElement(PackagingElement<?> element) {
        return myParentElements.get(element);
    }

    @Nullable
    public CompositePackagingElementNode getParentNode() {
        return myParentNode;
    }

    public List<E> getPackagingElements() {
        return myPackagingElements;
    }

    @Nullable
    public E getElementIfSingle() {
        return myPackagingElements.size() == 1 ? myPackagingElements.get(0) : null;
    }

    @Nonnull
    @Override
    public Object[] getEqualityObjects() {
        return ArrayUtil.toObjectArray(myPackagingElements);
    }

    @Override
    protected SimpleNode[] buildChildren() {
        return SimpleNode.NO_CHILDREN;
    }

    public E getFirstElement() {
        return myPackagingElements.get(0);
    }

    @Override
    protected void update(PresentationData presentation) {
        Collection<ArtifactProblemDescription> problems =
            ((ArtifactEditorImpl) myContext.getThisArtifactEditor()).getValidationManager().getProblems(this);
        if (problems == null || problems.isEmpty()) {
            super.update(presentation);
            return;
        }
        StringBuilder buffer = new StringBuilder();
        String tooltip;
        boolean isError = false;
        for (ArtifactProblemDescription problem : problems) {
            isError |= problem.getSeverity() == ProjectStructureProblemType.Severity.ERROR;
            buffer.append(problem.getMessage(false)).append("<br>");
        }
        tooltip = XmlStringUtil.wrapInHtml(buffer);

        getElementPresentation().render(presentation, addErrorHighlighting(isError, SimpleTextAttributes.REGULAR_ATTRIBUTES),
            addErrorHighlighting(isError, SimpleTextAttributes.GRAY_ATTRIBUTES)
        );
        presentation.setTooltip(tooltip);
    }

    private static SimpleTextAttributes addErrorHighlighting(boolean error, SimpleTextAttributes attributes) {
        TextAttributes textAttributes = TextAttributesUtil.toTextAttributes(attributes);
        textAttributes.setEffectType(EffectType.WAVE_UNDERSCORE);
        textAttributes.setEffectColor(error ? StandardColors.RED : StandardColors.GRAY);
        return TextAttributesUtil.fromTextAttributes(textAttributes);
    }

    @SuppressWarnings("unchecked")
    void addElement(PackagingElement<?> element, CompositePackagingElement parentElement, Collection<PackagingNodeSource> nodeSource) {
        doAddElement((E) element);
        myParentElements.put(element, parentElement);
        myNodeSources.putAll(element, nodeSource);
    }

    @Nonnull
    public Collection<PackagingNodeSource> getNodeSources() {
        return myNodeSources.values();
    }

    @Nonnull
    public Collection<PackagingNodeSource> getNodeSource(@Nonnull PackagingElement<?> element) {
        Collection<PackagingNodeSource> nodeSources = myNodeSources.get(element);
        return nodeSources != null ? nodeSources : Collections.<PackagingNodeSource>emptyList();
    }

    public ArtifactEditorContext getContext() {
        return myContext;
    }

    @Nullable
    public CompositePackagingElementNode findCompositeChild(@Nonnull String name) {
        SimpleNode[] children = getChildren();
        for (SimpleNode child : children) {
            if (child instanceof CompositePackagingElementNode) {
                CompositePackagingElementNode composite = (CompositePackagingElementNode) child;
                if (name.equals(composite.getFirstElement().getName())) {
                    return composite;
                }
            }
        }
        return null;
    }


    public List<PackagingElementNode<?>> getNodesByPath(List<PackagingElement<?>> pathToPlace) {
        List<PackagingElementNode<?>> result = new ArrayList<>();
        PackagingElementNode<?> current = this;
        int i = 0;
        result.add(current);
        while (current != null && i < pathToPlace.size()) {
            SimpleNode[] children = current.getCached();
            if (children == null) {
                break;
            }

            PackagingElementNode<?> next = null;
            PackagingElement<?> element = pathToPlace.get(i);

            search:
            for (SimpleNode child : children) {
                if (child instanceof PackagingElementNode<?>) {
                    PackagingElementNode<?> childNode = (PackagingElementNode<?>) child;
                    for (PackagingElement<?> childElement : childNode.getPackagingElements()) {
                        if (childElement.isEqualTo(element)) {
                            next = childNode;
                            break search;
                        }
                    }
                    for (PackagingNodeSource nodeSource : childNode.getNodeSources()) {
                        if (nodeSource.getSourceElement().isEqualTo(element)) {
                            next = current;
                            break search;
                        }
                    }
                }
            }
            current = next;
            if (current != null) {
                result.add(current);
            }
            i++;
        }
        return result;
    }
}
