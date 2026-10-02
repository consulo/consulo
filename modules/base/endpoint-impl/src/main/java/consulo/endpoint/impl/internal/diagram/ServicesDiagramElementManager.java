package consulo.endpoint.impl.internal.diagram;

import consulo.dataContext.DataContext;
import consulo.diagram.AbstractDiagramElementManager;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

final class ServicesDiagramElementManager extends AbstractDiagramElementManager<ServicesDiagramElement> {
    private static final Object[] NO_ITEMS = new Object[0];

    @Override
    public @Nullable ServicesDiagramElement findInDataContext(DataContext context) {
        return context.getData(ServicesDiagramRoot.KEY);
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof ServicesDiagramElement;
    }

    @Override
    public Object[] getNodeItems(ServicesDiagramElement parent) {
        if (parent instanceof ServicesDiagramService service) {
            return service.members().toArray();
        }
        if (parent instanceof ServicesDiagramExternal external) {
            return external.members().toArray();
        }
        return NO_ITEMS;
    }

    @Override
    public @Nullable Image getNodeElementIcon(Object element) {
        return element instanceof ServicesDiagramMember member ? member.icon() : null;
    }

    @Override
    public boolean canCollapse(ServicesDiagramElement element) {
        return false;
    }

    @Override
    public boolean isContainerFor(ServicesDiagramElement container, ServicesDiagramElement element) {
        return false;
    }

    @Override
    public String getElementTitle(ServicesDiagramElement element) {
        return element.getName();
    }

    @Override
    public @Nullable SimpleColoredText getPresentableName(Object element) {
        if (element instanceof ServicesDiagramMember member) {
            return new SimpleColoredText(member.text(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
        return null;
    }

    @Override
    public @Nullable SimpleColoredText getPresentableType(Object element) {
        if (element instanceof ServicesDiagramMember member && !member.framework().isEmpty()) {
            return new SimpleColoredText(member.framework(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        return null;
    }

    @Override
    public @Nullable String getElementDescription(ServicesDiagramElement element) {
        return null;
    }

    @Override
    public @Nullable String getNodeTooltip(ServicesDiagramElement element) {
        return element.getName();
    }
}
