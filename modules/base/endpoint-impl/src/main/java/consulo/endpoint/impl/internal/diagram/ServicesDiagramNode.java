package consulo.endpoint.impl.internal.diagram;

import consulo.diagram.DiagramNodeBase;
import consulo.diagram.DiagramProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

final class ServicesDiagramNode extends DiagramNodeBase<ServicesDiagramElement> {
    private final ServicesDiagramElement myElement;
    private final @Nullable Image myIcon;

    ServicesDiagramNode(DiagramProvider<ServicesDiagramElement> provider, ServicesDiagramElement element, @Nullable Image icon) {
        super(provider);
        myElement = element;
        myIcon = icon;
    }

    @Override
    public String getName() {
        return myElement.getName();
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public ServicesDiagramElement getIdentifyingElement() {
        return myElement;
    }
}
