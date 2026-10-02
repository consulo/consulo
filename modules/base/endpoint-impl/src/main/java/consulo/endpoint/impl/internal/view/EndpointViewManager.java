package consulo.endpoint.impl.internal.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.endpoint.EndpointProjectModel;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "EndpointView", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class EndpointViewManager implements PersistentStateComponent<EndpointViewState> {
    private final Project myProject;

    private EndpointViewState myState = new EndpointViewState();
    private @Nullable EndpointView myView;
    private @Nullable String myPendingSearchText;

    @Inject
    public EndpointViewManager(Project project) {
        myProject = project;
    }

    public static EndpointViewManager getInstance(Project project) {
        return project.getInstance(EndpointViewManager.class);
    }

    public static @Nullable EndpointViewManager getInstance(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project == null || project.isDisposed() ? null : getInstance(project);
    }

    @Override
    public EndpointViewState getState() {
        EndpointView view = myView;
        if (view != null) {
            view.saveState(myState);
        }
        return myState;
    }

    @Override
    public void loadState(EndpointViewState state) {
        myState = state;
    }

    public EndpointQuery createQuery(@Nullable String searchText) {
        return EndpointQuery.capture(myState, searchText);
    }

    public EndpointProjectModel getProjectModel() {
        EndpointProjectModel model = myProject.getExtensionPoint(EndpointProjectModel.class).findFirstSafe(it -> true);
        return model != null ? model : new DefaultEndpointProjectModel(myProject);
    }

    public @Nullable EndpointView getView() {
        return myView;
    }

    @RequiredUIAccess
    void attachView(EndpointView view) {
        myView = view;

        String searchText = myPendingSearchText;
        if (searchText != null) {
            myPendingSearchText = null;
            view.setSearchText(searchText);
        }
    }

    void detachView(EndpointView view) {
        if (myView == view) {
            myView = null;
        }
    }

    public boolean isCompact() {
        return myState.compact;
    }

    @RequiredUIAccess
    public void setCompact(boolean compact) {
        myState.compact = compact;
        fireOptionsChanged(false);
    }

    public boolean isGroupByModule() {
        return myState.groupByModule;
    }

    @RequiredUIAccess
    public void setGroupByModule(boolean groupByModule) {
        myState.groupByModule = groupByModule;
        fireOptionsChanged(false);
    }

    public boolean isFromLibraries() {
        return myState.fromLibraries;
    }

    @RequiredUIAccess
    public void setFromLibraries(boolean fromLibraries) {
        myState.fromLibraries = fromLibraries;
        fireOptionsChanged(true);
    }

    public boolean isFromTests() {
        return myState.fromTests;
    }

    @RequiredUIAccess
    public void setFromTests(boolean fromTests) {
        myState.fromTests = fromTests;
        fireOptionsChanged(true);
    }

    public boolean isModuleVisible(String moduleName) {
        return !myState.hiddenModules.contains(moduleName);
    }

    @RequiredUIAccess
    public void setModuleVisible(String moduleName, boolean visible) {
        if (visible) {
            myState.hiddenModules.remove(moduleName);
        }
        else {
            myState.hiddenModules.add(moduleName);
        }
        fireOptionsChanged(true);
    }

    public boolean isExternalVisible() {
        return myState.showExternal;
    }

    @RequiredUIAccess
    public void setExternalVisible(boolean visible) {
        myState.showExternal = visible;
        fireOptionsChanged(true);
    }

    public boolean isTypeVisible(String typeTag) {
        return !myState.hiddenTypes.contains(typeTag);
    }

    @RequiredUIAccess
    public void setTypeVisible(String typeTag, boolean visible) {
        if (visible) {
            myState.hiddenTypes.remove(typeTag);
        }
        else {
            myState.hiddenTypes.add(typeTag);
        }
        fireOptionsChanged(false);
    }

    public boolean isFrameworkVisible(String frameworkTag) {
        return !myState.hiddenFrameworks.contains(frameworkTag);
    }

    @RequiredUIAccess
    public void setFrameworkVisible(String frameworkTag, boolean visible) {
        if (visible) {
            myState.hiddenFrameworks.remove(frameworkTag);
        }
        else {
            myState.hiddenFrameworks.add(frameworkTag);
        }
        fireOptionsChanged(false);
    }

    @RequiredUIAccess
    public void resetFilters() {
        clearFilters();
        fireOptionsChanged(true);
    }

    public boolean isDetailsVisible() {
        return myState.detailsVisible;
    }

    public int getSplitProportion() {
        return myState.splitProportion;
    }

    public void setSplitProportion(int splitProportion) {
        myState.splitProportion = splitProportion;
    }

    public @Nullable String getSelectedTabId() {
        return myState.selectedTabId;
    }

    @RequiredUIAccess
    public void showEndpoints(@Nullable String module, @Nullable String framework, @Nullable String filter) {
        clearFilters();

        StringBuilder builder = new StringBuilder();
        if (module != null && !module.isEmpty()) {
            builder.append(getProjectModel().getModuleQueryTag()).append(':').append(EndpointSearchQuery.quote(module));
        }
        if (framework != null && !framework.isEmpty()) {
            appendSeparator(builder);
            builder.append(EndpointSearchQuery.FRAMEWORK_TAG).append(':').append(EndpointSearchQuery.quote(framework));
        }
        if (filter != null && !filter.isEmpty()) {
            appendSeparator(builder);
            builder.append(filter);
        }

        String searchText = builder.toString();
        EndpointView view = myView;
        if (view == null) {
            myPendingSearchText = searchText;
            return;
        }

        view.setSearchText(searchText);
        view.optionsChanged(true);
    }

    private static void appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
    }

    private void clearFilters() {
        myState.hiddenModules.clear();
        myState.hiddenTypes.clear();
        myState.hiddenFrameworks.clear();
        myState.showExternal = true;
    }

    @RequiredUIAccess
    private void fireOptionsChanged(boolean reload) {
        EndpointView view = myView;
        if (view != null) {
            view.optionsChanged(reload);
        }
    }
}
