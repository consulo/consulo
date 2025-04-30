// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ServiceViewContributor<T> {
    @Nullable
    static <V extends ServiceViewContributor<?>> V findRootContributor(@Nonnull Class<V> contributorClass) {
        return Application.get().getExtensionPoint(ServiceViewContributor.class).findExtension(contributorClass);
    }

    @Nonnull
    ServiceViewDescriptor getViewDescriptor(@Nonnull Project project);

    @Nonnull
    List<T> getServices(@Nonnull Project project);

    @Nonnull
    ServiceViewDescriptor getServiceDescriptor(@Nonnull Project project, @Nonnull T service);
}
