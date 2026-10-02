// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.project.Project;

import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public interface ClientGeneratorOpenInScratchService {
    String CLIENT_EXAMPLE_SCRATCH_PREFIX = "client-example";

    static ClientGeneratorOpenInScratchService getInstance(Project project) {
        return project.getInstance(ClientGeneratorOpenInScratchService.class);
    }

    CompletableFuture<?> createScratchFileWithoutEndpointsChangeTracking(ClientGenerator clientGenerator, OpenApiSpecification oas);

    CompletableFuture<?> createScratchFile(ClientGenerator clientGenerator, Editor editor, UrlPathContext urlPathContext);

    CompletableFuture<?> createScratchFile(ClientGenerator clientGenerator, OpenApiSpecification oas);
}
