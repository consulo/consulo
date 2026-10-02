package consulo.endpoint.impl.internal.view.detail;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointSidePanel;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import org.jspecify.annotations.Nullable;

final class EndpointSidePanelTab {
    private final String myId;
    private final EndpointSidePanel myPanel;
    private final Tab myTab;
    private final LoadingLayout<DockLayout> myLoadingLayout;

    private boolean myAvailable;
    private boolean myUpdated;
    private @Nullable Disposable myUpdateDisposable;

    EndpointSidePanelTab(String id, EndpointSidePanel panel, Tab tab, LoadingLayout<DockLayout> loadingLayout) {
        myId = id;
        myPanel = panel;
        myTab = tab;
        myLoadingLayout = loadingLayout;
    }

    String getId() {
        return myId;
    }

    EndpointSidePanel getPanel() {
        return myPanel;
    }

    Tab getTab() {
        return myTab;
    }

    boolean isAvailable() {
        return myAvailable;
    }

    void setAvailable(boolean available) {
        myAvailable = available;
    }

    boolean isUpdated() {
        return myUpdated;
    }

    boolean isUpdating() {
        return myUpdateDisposable != null;
    }

    boolean isCurrentUpdate(Disposable updateDisposable) {
        return myUpdateDisposable == updateDisposable;
    }

    @RequiredUIAccess
    void startUpdate(Disposable updateDisposable) {
        myUpdated = false;
        myUpdateDisposable = updateDisposable;
        myLoadingLayout.startLoading();
    }

    @RequiredUIAccess
    void finishUpdate(Disposable updateDisposable, boolean updated) {
        if (myUpdateDisposable != updateDisposable) {
            return;
        }
        myUpdateDisposable = null;
        myUpdated = updated;
        myLoadingLayout.stopLoading(layout -> layout.center(myPanel.getComponent()));
    }

    @RequiredUIAccess
    void cancelUpdate() {
        Disposable updateDisposable = myUpdateDisposable;
        if (updateDisposable == null) {
            return;
        }
        myUpdateDisposable = null;
        myUpdated = false;
        Disposer.dispose(updateDisposable);
        myLoadingLayout.stopLoading(layout -> layout.center(myPanel.getComponent()));
    }

    @RequiredUIAccess
    void reset() {
        cancelUpdate();
        myUpdated = false;
    }

    @RequiredUIAccess
    void updateEnabled(boolean selected) {
        myTab.setEnabled(myAvailable || selected);
    }
}
