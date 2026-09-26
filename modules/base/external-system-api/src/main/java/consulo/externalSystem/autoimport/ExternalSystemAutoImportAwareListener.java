// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * This is a temporary workaround.
 * This API will be removed in 2026.3 - IDEA-389819.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface ExternalSystemAutoImportAwareListener {
    void autoImportAwareOperationStarted();

    void autoImportAwareOperationCompleted();
}
