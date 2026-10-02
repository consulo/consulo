package consulo.endpoint.impl.internal.view;

import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointProvider;

public record EndpointCacheKey(EndpointProvider<?, ?> provider, EndpointFilter filter) {
}
