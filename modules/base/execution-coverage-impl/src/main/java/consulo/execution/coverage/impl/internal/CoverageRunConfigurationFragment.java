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
package consulo.execution.coverage.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentType;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentGroup;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.localize.LocalizeValue;

/**
 * The "Code Coverage" section of configurations a coverage engine applies to. Its fragments come from the engines (for example
 * class filters); the runner and the collecting mode are project coverage settings.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
@ExtensionImpl
public class CoverageRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return StandardRunConfigurationFragmentGroup.COVERAGE;
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionCoverageLocalize.runConfigurationFragmentCoverage();
    }

    @Override
    public LocalizeValue getGroup() {
        return ExecutionCoverageLocalize.runConfigurationFragmentGroupCodeCoverage();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.COVERAGE;
    }

    @Override
    public RunConfigurationFragmentType getType() {
        return RunConfigurationFragmentType.GROUP;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof RunConfigurationBase base && CoverageEnabledConfiguration.isApplicableTo(base);
    }
}
