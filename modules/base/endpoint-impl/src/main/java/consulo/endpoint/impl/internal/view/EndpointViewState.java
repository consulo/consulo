package consulo.endpoint.impl.internal.view;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class EndpointViewState {
    public boolean compact;
    public boolean groupByModule;
    public boolean fromLibraries;
    public boolean fromTests;
    public boolean showExternal = true;
    public Set<String> hiddenModules = new LinkedHashSet<>();
    public Set<String> hiddenTypes = new LinkedHashSet<>();
    public Set<String> hiddenFrameworks = new LinkedHashSet<>();
    public boolean detailsVisible = true;
    public int splitProportion = 50;
    public @Nullable String selectedTabId;
}
