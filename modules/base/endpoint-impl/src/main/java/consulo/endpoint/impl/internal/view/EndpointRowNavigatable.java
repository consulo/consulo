package consulo.endpoint.impl.internal.view;

import consulo.navigation.Navigatable;

public final class EndpointRowNavigatable implements Navigatable {
    private final EndpointView myView;
    private final EndpointRow<?, ?> myRow;

    public EndpointRowNavigatable(EndpointView view, EndpointRow<?, ?> row) {
        myView = view;
        myRow = row;
    }

    @Override
    public void navigate(boolean requestFocus) {
        myView.navigate(myRow, requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return true;
    }
}
