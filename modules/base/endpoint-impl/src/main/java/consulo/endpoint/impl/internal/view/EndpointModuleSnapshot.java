package consulo.endpoint.impl.internal.view;

import consulo.endpoint.EndpointModuleEntity;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class EndpointModuleSnapshot {
    public static final String EXTERNAL_KEY = "<external>";

    private final @Nullable EndpointModuleEntity myModule;
    private final String myKey;
    private final LocalizeValue myName;
    private final @Nullable Image myIcon;
    private final List<EndpointRowData<?, ?>> myRows;

    public EndpointModuleSnapshot(
        @Nullable EndpointModuleEntity module,
        String key,
        LocalizeValue name,
        @Nullable Image icon,
        List<EndpointRowData<?, ?>> rows
    ) {
        myModule = module;
        myKey = key;
        myName = name;
        myIcon = icon;
        myRows = rows;
    }

    public @Nullable EndpointModuleEntity getModule() {
        return myModule;
    }

    public String getKey() {
        return myKey;
    }

    public LocalizeValue getName() {
        return myName;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public List<EndpointRowData<?, ?>> getRows() {
        return myRows;
    }

    public boolean isExternal() {
        return myModule == null;
    }
}
