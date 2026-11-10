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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.externalService.statistic.InvocationCounter;
import jakarta.inject.Singleton;

/**
 * Tracks nothing and advertises nothing.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceImpl
@Singleton
public class HeadlessFeatureUsageTracker extends FeatureUsageTracker {
    private static final InvocationCounter NO_COUNTER = () -> {
    };

    @Override
    public void triggerFeatureUsed(String featureId) {
    }

    @Override
    public void triggerFeatureShown(String featureId) {
    }

    @Override
    public boolean isToBeShown(String featureId, ComponentManager project) {
        return false;
    }

    @Override
    public boolean isToBeAdvertisedInLookup(String featureId, ComponentManager project) {
        return false;
    }

    @Override
    public InvocationCounter getCompletionStatistics() {
        return NO_COUNTER;
    }

    @Override
    public InvocationCounter getFixesStats() {
        return NO_COUNTER;
    }
}
