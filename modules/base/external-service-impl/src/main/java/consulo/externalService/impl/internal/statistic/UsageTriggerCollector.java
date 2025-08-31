/*
 * Copyright 2013-2022 consulo.io
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
import consulo.externalService.statistic.UsageDescriptor;
import consulo.externalService.statistic.UsageTrigger;
import consulo.externalService.statistic.UsagesCollector;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 10-Jul-22
 */
@ExtensionImpl
public class UsageTriggerCollector extends UsagesCollector {
  private final Provider<UsageTrigger> myUsageTrigger;

  @Inject
  public UsageTriggerCollector(Provider<UsageTrigger> usageTrigger) {
    myUsageTrigger = usageTrigger;
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) {
    UsageTriggerImpl.State state = ((UsageTriggerImpl)myUsageTrigger.get()).getState();

    return ContainerUtil.map2Set(state.myValues.entrySet(), e -> new UsageDescriptor(e.getKey(), e.getValue()));
  }

  @Override
  @Nonnull
  public String getGroupId() {
    return "consulo.platform.base:features.count";
  }
}