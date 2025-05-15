/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.externalService.impl.internal.statistic;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.externalService.statistic.UsagesCollector;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-05-15
 */
@ExtensionImpl
public class SystemUsageCollector extends UsagesCollector {
    @Nonnull
    @Override
    public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
        Platform current = Platform.current();
        PlatformOperatingSystem os = current.os();
        CpuArchitecture arch = current.jvm().arch();
        return Set.of(new UsageDescriptor(os.name() + " - " + arch.name(), 1));
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return "consulo:system";
    }
}
