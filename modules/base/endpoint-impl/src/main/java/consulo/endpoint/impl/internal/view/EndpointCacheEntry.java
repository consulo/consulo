package consulo.endpoint.impl.internal.view;

import java.util.List;

public record EndpointCacheEntry(long stamp, long generation, List<EndpointRowData<?, ?>> rows) {
}
