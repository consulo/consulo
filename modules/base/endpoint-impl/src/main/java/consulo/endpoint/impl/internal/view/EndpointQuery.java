package consulo.endpoint.impl.internal.view;

import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class EndpointQuery {
    private final Set<String> myHiddenModules;
    private final boolean myShowExternal;
    private final boolean myFromLibraries;
    private final boolean myFromTests;
    private final Set<String> myHiddenTypes;
    private final Set<String> myHiddenFrameworks;
    private final boolean myCompact;
    private final boolean myGroupByModule;
    private final String mySearchText;

    private EndpointQuery(EndpointViewState state, String searchText) {
        myHiddenModules = Set.copyOf(state.hiddenModules);
        myShowExternal = state.showExternal;
        myFromLibraries = state.fromLibraries;
        myFromTests = state.fromTests;
        myHiddenTypes = Set.copyOf(state.hiddenTypes);
        myHiddenFrameworks = Set.copyOf(state.hiddenFrameworks);
        myCompact = state.compact;
        myGroupByModule = state.groupByModule;
        mySearchText = searchText;
    }

    public static EndpointQuery capture(EndpointViewState state, @Nullable String searchText) {
        return new EndpointQuery(state, searchText == null ? "" : searchText);
    }

    public Set<String> getHiddenModules() {
        return myHiddenModules;
    }

    public boolean isShowExternal() {
        return myShowExternal;
    }

    public boolean isFromLibraries() {
        return myFromLibraries;
    }

    public boolean isFromTests() {
        return myFromTests;
    }

    public Set<String> getHiddenTypes() {
        return myHiddenTypes;
    }

    public Set<String> getHiddenFrameworks() {
        return myHiddenFrameworks;
    }

    public boolean isCompact() {
        return myCompact;
    }

    public boolean isGroupByModule() {
        return myGroupByModule;
    }

    public String getSearchText() {
        return mySearchText;
    }

    public boolean isLoadFiltered() {
        return !myHiddenModules.isEmpty() || !myShowExternal;
    }

    public boolean isSameLoad(@Nullable EndpointQuery other) {
        return other != null
            && myShowExternal == other.myShowExternal
            && myFromLibraries == other.myFromLibraries
            && myFromTests == other.myFromTests
            && myHiddenModules.equals(other.myHiddenModules);
    }
}
