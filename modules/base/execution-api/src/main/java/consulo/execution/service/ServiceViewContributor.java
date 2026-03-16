// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ServiceViewContributor<T> {
    @Nullable
    static <V extends ServiceViewContributor<?>> V findRootContributor(Class<V> contributorClass) {
        return Application.get().getExtensionPoint(ServiceViewContributor.class).findExtension(contributorClass);
    }

    
    ServiceViewDescriptor getViewDescriptor(Project project);

    
    List<T> getServices(Project project);

    
    ServiceViewDescriptor getServiceDescriptor(Project project, T service);
}
