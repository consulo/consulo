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

import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandProfilerConfigurable extends SimpleConfigurableByProperties {
    private final SandProfilerConfigurationState myState;

    public SandProfilerConfigurable(SandProfilerConfigurationState state) {
        myState = state;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        IntBox samplingIntervalBox = IntBox.create(SandProfilerConfigurationState.DEFAULT_SAMPLING_INTERVAL_MS)
            .withRange(SandProfilerConfigurationState.MIN_SAMPLING_INTERVAL_MS, SandProfilerConfigurationState.MAX_SAMPLING_INTERVAL_MS);
        propertyBuilder.add(
            samplingIntervalBox,
            myState::getSamplingIntervalMs,
            value -> myState.setSamplingIntervalMs(value == null ? SandProfilerConfigurationState.DEFAULT_SAMPLING_INTERVAL_MS : value)
        );

        IntBox threadCountBox = IntBox.create(SandProfilerConfigurationState.DEFAULT_SYNTHETIC_THREAD_COUNT)
            .withRange(
                SandProfilerConfigurationState.MIN_SYNTHETIC_THREAD_COUNT,
                SandProfilerConfigurationState.MAX_SYNTHETIC_THREAD_COUNT
            );
        propertyBuilder.add(
            threadCountBox,
            myState::getSyntheticThreadCount,
            value -> myState.setSyntheticThreadCount(
                value == null ? SandProfilerConfigurationState.DEFAULT_SYNTHETIC_THREAD_COUNT : value
            )
        );

        return FormBuilder.create()
            .addLabeled(LocalizeValue.localizeTODO("Sampling interval (ms):"), samplingIntervalBox)
            .addLabeled(LocalizeValue.localizeTODO("Synthetic threads:"), threadCountBox)
            .build();
    }
}
