package consulo.endpoint.impl.internal.view;

import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointType;
import consulo.endpoint.FrameworkPresentation;

public record EndpointProviderInfo(EndpointProvider<?, ?> provider, EndpointType type, FrameworkPresentation framework) {
}
