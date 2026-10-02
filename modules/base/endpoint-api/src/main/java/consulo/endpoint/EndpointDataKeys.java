package consulo.endpoint;

import consulo.util.dataholder.Key;

import java.util.List;

public final class EndpointDataKeys {
    public static final Key<List<EndpointListItem>> SELECTED_ITEMS = Key.create("endpoint.selectedItems");

    private EndpointDataKeys() {
    }
}
