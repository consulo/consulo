package consulo.endpoint.impl.internal.diagram;

import consulo.annotation.access.RequiredReadAction;
import consulo.diagram.DiagramVfsResolver;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

final class ServicesDiagramVfsResolver implements DiagramVfsResolver<ServicesDiagramElement> {
    @Override
    public String getQualifiedName(ServicesDiagramElement element) {
        return element.getQualifiedName();
    }

    @RequiredReadAction
    @Override
    public @Nullable ServicesDiagramElement resolveElementByFQN(String fqn, Project project) {
        if (ServicesDiagramRoot.QUALIFIED_NAME.equals(fqn)) {
            return new ServicesDiagramRoot(project.getName());
        }
        return ServicesDiagramGraphBuilder.getGraph(project).findElement(fqn);
    }
}
