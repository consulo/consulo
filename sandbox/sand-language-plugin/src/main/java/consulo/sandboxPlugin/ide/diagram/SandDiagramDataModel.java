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
package consulo.sandboxPlugin.ide.diagram;

import consulo.application.AllIcons;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramEdge;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramProvider;
import consulo.diagram.DiagramRelationshipInfo;
import consulo.diagram.DiagramRelationships;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class SandDiagramDataModel extends DiagramDataModel<String> {
    private final DiagramProvider<String> myProvider;
    private final List<DiagramNode<String>> myNodes = new ArrayList<>();
    private final List<DiagramEdge<String>> myEdges = new ArrayList<>();

    public SandDiagramDataModel(DiagramProvider<String> provider) {
        myProvider = provider;

        DiagramNode<String> object = node("Object", AllIcons.Nodes.Class);
        DiagramNode<String> collection = node("Collection", AllIcons.Nodes.Interface);
        DiagramNode<String> list = node("List", AllIcons.Nodes.Interface);
        DiagramNode<String> set = node("Set", AllIcons.Nodes.Interface);
        DiagramNode<String> arrayList = node("ArrayList", AllIcons.Nodes.Class);
        DiagramNode<String> hashSet = node("HashSet", AllIcons.Nodes.Class);

        edge(collection, object, DiagramRelationships.DEPENDENCY);
        edge(list, collection, DiagramRelationships.INTERFACE_GENERALIZATION);
        edge(set, collection, DiagramRelationships.INTERFACE_GENERALIZATION);
        edge(arrayList, list, DiagramRelationships.REALIZATION);
        edge(hashSet, set, DiagramRelationships.REALIZATION);
        edge(arrayList, object, DiagramRelationships.GENERALIZATION);
    }

    private DiagramNode<String> node(String name, Image icon) {
        DiagramNode<String> node = new SandDiagramNode(myProvider, name, icon);
        myNodes.add(node);
        return node;
    }

    private void edge(DiagramNode<String> source, DiagramNode<String> target, DiagramRelationshipInfo relationship) {
        myEdges.add(new SandDiagramEdge(source, target, relationship));
    }

    @Override
    public Collection<DiagramNode<String>> getNodes() {
        return myNodes;
    }

    @Override
    public Collection<DiagramEdge<String>> getEdges() {
        return myEdges;
    }

    @Override
    public DiagramNode<String> getSourceNode(DiagramEdge<String> edge) {
        return edge.getSource();
    }

    @Override
    public DiagramNode<String> getTargetNode(DiagramEdge<String> edge) {
        return edge.getTarget();
    }

    @Override
    public String getNodeName(DiagramNode<String> node) {
        String name = node.getName();
        return name == null ? node.getIdentifyingElement() : name;
    }

    @Override
    public String getEdgeName(DiagramEdge<String> edge) {
        return edge.getName();
    }

    @Override
    public @Nullable DiagramEdge<String> createEdge(DiagramNode<String> from, DiagramNode<String> to) {
        return null;
    }

    @Override
    public void removeNode(DiagramNode<String> node) {
        myNodes.remove(node);
        myEdges.removeIf(edge -> edge.getSource().equals(node) || edge.getTarget().equals(node));
    }

    @Override
    public @Nullable DiagramNode<String> addElement(String element) {
        return null;
    }

    @Override
    public void removeEdge(DiagramEdge<String> edge) {
        myEdges.remove(edge);
    }

    @Override
    public boolean hasElement(String element) {
        return myNodes.stream().anyMatch(node -> node.getIdentifyingElement().equals(element));
    }

    @Override
    public void dispose() {
    }
}
