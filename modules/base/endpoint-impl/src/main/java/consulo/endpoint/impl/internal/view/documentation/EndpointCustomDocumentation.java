package consulo.endpoint.impl.internal.view.documentation;

import consulo.disposer.Disposable;
import consulo.endpoint.EndpointDocumentationProvider;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

record EndpointCustomDocumentation<R>(EndpointDocumentationProvider<?, ?, R> provider, R request)
    implements EndpointDocumentationContent {

    @RequiredUIAccess
    @Nullable Component createComponent(Disposable parentDisposable) {
        return provider.getEndpointDocumentation(request, parentDisposable);
    }
}
