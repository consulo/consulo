/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.internal.statistic.configurable;

import com.intellij.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import com.intellij.internal.statistic.updater.StatisticsSendManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class StatisticsConfigurable implements SearchableConfigurable {
  private StatisticsConfigurationComponent myConfig;

  private final UsageStatisticsPersistenceComponent myUsageStatisticsPersistenceComponent;
  private final StatisticsSendManager myStatisticsSendManager;

  @Inject
  public StatisticsConfigurable(UsageStatisticsPersistenceComponent usageStatisticsPersistenceComponent, StatisticsSendManager statisticsSendManager) {
    myUsageStatisticsPersistenceComponent = usageStatisticsPersistenceComponent;
    myStatisticsSendManager = statisticsSendManager;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Usage Statistics";
  }

  @Override
  @Nullable
  public String getHelpTopic() {
    return "preferences.usage.statictics";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    myConfig = new StatisticsConfigurationComponent();
    return myConfig.getJComponent();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myConfig.isAllowed() != myUsageStatisticsPersistenceComponent.isAllowed() || myConfig.getPeriod() != myUsageStatisticsPersistenceComponent.getPeriod();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myUsageStatisticsPersistenceComponent.setPeriod(myConfig.getPeriod());
    myUsageStatisticsPersistenceComponent.setAllowed(myConfig.isAllowed());

    myStatisticsSendManager.sheduleRunIfStarted();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myConfig.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myConfig = null;
  }

  @Nonnull
  @Override
  public String getId() {
    return "usage.statistics";
  }
}
