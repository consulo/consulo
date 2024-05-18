// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.dashboard;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Register this extension to provide run configuration types available in Run Dashboard/Services by default.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunDashboardDefaultTypesProvider {
  @Nonnull
  Collection<String> getDefaultTypeIds(@Nonnull Project project);
}
