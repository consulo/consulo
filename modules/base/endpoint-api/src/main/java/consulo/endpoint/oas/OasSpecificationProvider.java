// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides OpenAPI description for URL targets to complete request body parameters in HTTP Client and JavaScript.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface OasSpecificationProvider {
    @RequiredReadAction
    @Nullable OpenApiSpecification getOasSpecification(UrlTargetInfo urlTargetInfo);

    /**
     * @return a generated VirtualFile with the OpenAPI specification from {@code urlTargetInfo}, wrapped into given {@code prefixes}
     * @param prefixes the list of schema property prefixes to wrap the specification
     */
    @RequiredReadAction
    @Nullable VirtualFile getOasSpecificationFile(UrlTargetInfo urlTargetInfo, List<String> prefixes);

    @RequiredReadAction
    static @Nullable OpenApiSpecification findOasSpecification(UrlTargetInfo urlTargetInfo) {
        return Application.get()
            .getExtensionPoint(OasSpecificationProvider.class)
            .computeSafeIfAny(provider -> provider.getOasSpecification(urlTargetInfo));
    }
}
