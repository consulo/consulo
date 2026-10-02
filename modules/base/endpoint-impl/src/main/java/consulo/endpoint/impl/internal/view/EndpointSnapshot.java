package consulo.endpoint.impl.internal.view;

import java.util.List;

public final class EndpointSnapshot {
    public static final EndpointSnapshot EMPTY = new EndpointSnapshot(false, List.of(), List.of(), List.of(), List.of());

    private final boolean myHasProviders;
    private final List<EndpointModuleSnapshot> mySections;
    private final List<EndpointFilterChoice> myModules;
    private final List<EndpointFilterChoice> myTypes;
    private final List<EndpointFilterChoice> myFrameworks;

    public EndpointSnapshot(
        boolean hasProviders,
        List<EndpointModuleSnapshot> sections,
        List<EndpointFilterChoice> modules,
        List<EndpointFilterChoice> types,
        List<EndpointFilterChoice> frameworks
    ) {
        myHasProviders = hasProviders;
        mySections = sections;
        myModules = modules;
        myTypes = types;
        myFrameworks = frameworks;
    }

    public boolean hasProviders() {
        return myHasProviders;
    }

    public List<EndpointModuleSnapshot> getSections() {
        return mySections;
    }

    public List<EndpointFilterChoice> getModules() {
        return myModules;
    }

    public List<EndpointFilterChoice> getTypes() {
        return myTypes;
    }

    public List<EndpointFilterChoice> getFrameworks() {
        return myFrameworks;
    }

    public boolean hasRows() {
        for (EndpointModuleSnapshot section : mySections) {
            if (!section.getRows().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
