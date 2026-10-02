package consulo.endpoint.impl.internal.diagram;

public sealed interface ServicesDiagramElement permits ServicesDiagramRoot, ServicesDiagramService, ServicesDiagramExternal {
    String getName();

    String getQualifiedName();
}
