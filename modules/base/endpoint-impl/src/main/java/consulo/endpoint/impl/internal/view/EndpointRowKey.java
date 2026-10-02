package consulo.endpoint.impl.internal.view;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record EndpointRowKey(@Nullable String module, String provider, String url, List<String> methods, String location) {
    public EndpointRowKey withModule(@Nullable String moduleKey) {
        return new EndpointRowKey(moduleKey, provider, url, methods, location);
    }
}
