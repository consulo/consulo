// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * Provides information for an Examples tab in the Endpoints side panel.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ClientGenerator {
    LocalizeValue getTitle();

    default AvailableClientSettings getAvailableClientSettings() {
        return EmptyAvailableClientSettings.INSTANCE;
    }

    @Nullable ClientExample generate(OpenApiSpecification openApiSpecification);
}
