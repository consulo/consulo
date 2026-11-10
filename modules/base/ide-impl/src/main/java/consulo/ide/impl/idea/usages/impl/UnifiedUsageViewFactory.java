/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.usages.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.usage.UsageSearcher;
import consulo.usage.UsageTarget;
import consulo.usage.UsageViewPresentation;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedUsageViewFactory implements UsageViewFactory {
    private final Project myProject;

    @Inject
    public UnifiedUsageViewFactory(Project project) {
        myProject = project;
    }

    @Override
    public UsageViewInternal createUsageView(
        UsageViewPresentation presentation,
        UsageTarget[] targets,
        @Nullable Supplier<UsageSearcher> usageSearcherFactory
    ) {
        return new UnifiedUsageViewImpl(myProject, presentation, targets, usageSearcherFactory);
    }
}
