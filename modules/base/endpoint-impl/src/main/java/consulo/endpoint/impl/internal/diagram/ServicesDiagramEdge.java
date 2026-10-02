package consulo.endpoint.impl.internal.diagram;

import consulo.diagram.DiagramEdge;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramRelationshipInfo;
import consulo.diagram.DiagramRelationships;

final class ServicesDiagramEdge implements DiagramEdge<ServicesDiagramElement> {
    private final DiagramNode<ServicesDiagramElement> mySource;
    private final DiagramNode<ServicesDiagramElement> myTarget;
    private final int myCount;

    ServicesDiagramEdge(DiagramNode<ServicesDiagramElement> source, DiagramNode<ServicesDiagramElement> target, int count) {
        mySource = source;
        myTarget = target;
        myCount = count;
    }

    @Override
    public DiagramNode<ServicesDiagramElement> getSource() {
        return mySource;
    }

    @Override
    public DiagramNode<ServicesDiagramElement> getTarget() {
        return myTarget;
    }

    @Override
    public String getName() {
        return String.valueOf(myCount);
    }

    @Override
    public ServicesDiagramElement getIdentifyingElement() {
        return mySource.getIdentifyingElement();
    }

    @Override
    public DiagramRelationshipInfo getRelationship() {
        return DiagramRelationships.DEPENDENCY;
    }

    int getCount() {
        return myCount;
    }
}
