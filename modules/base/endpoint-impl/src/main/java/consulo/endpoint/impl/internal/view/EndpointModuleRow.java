package consulo.endpoint.impl.internal.view;

import consulo.endpoint.EndpointModuleEntity;
import consulo.endpoint.EndpointModuleItem;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public final class EndpointModuleRow implements EndpointViewRow, EndpointModuleItem {
    private final EndpointModuleSnapshot mySection;

    public EndpointModuleRow(EndpointModuleSnapshot section) {
        mySection = section;
    }

    @Override
    public @Nullable EndpointModuleEntity getModule() {
        return mySection.getModule();
    }

    public LocalizeValue getName() {
        return mySection.getName();
    }

    public @Nullable Image getIcon() {
        return mySection.getIcon();
    }

    public String getKey() {
        return mySection.getKey();
    }

    @Override
    public String getSpeedSearchText() {
        return mySection.getName().get();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof EndpointModuleRow that && getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        return "EndpointModuleRow(" + getKey() + ")";
    }
}
