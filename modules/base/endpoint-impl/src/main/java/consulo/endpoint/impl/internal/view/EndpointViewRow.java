package consulo.endpoint.impl.internal.view;

import consulo.endpoint.EndpointListItem;

public sealed interface EndpointViewRow extends EndpointListItem permits EndpointModuleRow, EndpointRow {
    String getSpeedSearchText();
}
