package consulo.endpoint.impl.internal.diagram;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

record ServicesDiagramGraph(
    List<ServicesDiagramElement> elements,
    Map<String, Image> icons,
    List<ServicesDiagramEdgeData> edges
) {
    static final ServicesDiagramGraph EMPTY = new ServicesDiagramGraph(List.of(), Map.of(), List.of());

    @Nullable ServicesDiagramElement findElement(String qualifiedName) {
        for (ServicesDiagramElement element : elements) {
            if (element.getQualifiedName().equals(qualifiedName)) {
                return element;
            }
        }
        return null;
    }

    @Nullable Image getIcon(ServicesDiagramElement element) {
        return icons.get(element.getQualifiedName());
    }
}
