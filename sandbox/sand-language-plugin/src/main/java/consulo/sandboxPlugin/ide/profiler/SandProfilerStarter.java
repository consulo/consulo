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
package consulo.sandboxPlugin.ide.profiler;

import consulo.execution.configuration.RunProfile;
import consulo.execution.profiler.configuration.ProfilerStarter;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.sandboxPlugin.ide.run.SandConfiguration;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandProfilerStarter implements ProfilerStarter {
    @Override
    public boolean canRun(RunProfile profile) {
        return profile instanceof SandConfiguration;
    }

    @Override
    public boolean isApplicable(Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
    }
}
