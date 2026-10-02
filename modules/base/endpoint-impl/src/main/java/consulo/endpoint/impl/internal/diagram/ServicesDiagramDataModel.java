package consulo.endpoint.impl.internal.diagram;

import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramEdge;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ServicesDiagramDataModel extends DiagramDataModel<ServicesDiagramElement> {
    private final Map<String, DiagramNode<ServicesDiagramElement>> myNodes = new LinkedHashMap<>();
    private final List<DiagramEdge<ServicesDiagramElement>> myEdges = new ArrayList<>();

    ServicesDiagramDataModel(DiagramProvider<ServicesDiagramElement> provider, ServicesDiagramGraph graph) {
        for (ServicesDiagramElement element : graph.elements()) {
            myNodes.put(element.getQualifiedName(), new ServicesDiagramNode(provider, element, graph.getIcon(element)));
        }
        for (ServicesDiagramEdgeData edge : graph.edges()) {
            DiagramNode<ServicesDiagramElement> source = myNodes.get(edge.sourceQualifiedName());
            DiagramNode<ServicesDiagramElement> target = myNodes.get(edge.targetQualifiedName());
            if (source != null && target != null && source != target) {
                myEdges.add(new ServicesDiagramEdge(source, target, edge.count()));
            }
        }
    }

    @Override
    public Collection<DiagramNode<ServicesDiagramElement>> getNodes() {
        return myNodes.values();
    }

    @Override
    public Collection<DiagramEdge<ServicesDiagramElement>> getEdges() {
        return myEdges;
    }

    @Override
    public DiagramNode<ServicesDiagramElement> getSourceNode(DiagramEdge<ServicesDiagramElement> edge) {
        return edge.getSource();
    }

    @Override
    public DiagramNode<ServicesDiagramElement> getTargetNode(DiagramEdge<ServicesDiagramElement> edge) {
        return edge.getTarget();
    }

    @Override
    public String getNodeName(DiagramNode<ServicesDiagramElement> node) {
        return node.getIdentifyingElement().getName();
    }

    @Override
    public String getEdgeName(DiagramEdge<ServicesDiagramElement> edge) {
        return edge.getName();
    }

    @Override
    public @Nullable DiagramEdge<ServicesDiagramElement> createEdge(
        DiagramNode<ServicesDiagramElement> from,
        DiagramNode<ServicesDiagramElement> to
    ) {
        return null;
    }

    @Override
    public void removeNode(DiagramNode<ServicesDiagramElement> node) {
        myNodes.values().remove(node);
        myEdges.removeIf(edge -> edge.getSource().equals(node) || edge.getTarget().equals(node));
    }

    @Override
    public @Nullable DiagramNode<ServicesDiagramElement> addElement(ServicesDiagramElement element) {
        return null;
    }

    @Override
    public void removeEdge(DiagramEdge<ServicesDiagramElement> edge) {
        myEdges.remove(edge);
    }

    @Override
    public boolean hasElement(ServicesDiagramElement element) {
        return myNodes.containsKey(element.getQualifiedName());
    }

    @Override
    public void dispose() {
    }
}
