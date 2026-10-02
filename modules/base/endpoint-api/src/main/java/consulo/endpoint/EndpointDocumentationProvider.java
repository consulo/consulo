// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

public interface EndpointDocumentationProvider<G, E, R> extends EndpointProvider<G, E> {
    @RequiredReadAction
    @Nullable R prepareDocumentationRequest(G group, E endpoint);

    @RequiredUIAccess
    @Nullable Component getEndpointDocumentation(R request, Disposable parentDisposable);
}
