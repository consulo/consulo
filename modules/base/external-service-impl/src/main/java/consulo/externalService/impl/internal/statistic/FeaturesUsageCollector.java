/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import consulo.externalService.statistic.FeatureDescriptor;
import consulo.externalService.statistic.ProductivityFeaturesRegistry;
import consulo.externalService.statistic.UsagesCollector;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
public class FeaturesUsageCollector extends UsagesCollector {

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:productivity";
  }

  @Nonnull
  @Override
  public Set<UsageDescriptor> getUsages(@Nullable Project project) {
    Set<UsageDescriptor> usages = new HashSet<UsageDescriptor>();

    ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
    for (String featureId : registry.getFeatureIds()) {
      FeatureDescriptor featureDescriptor = registry.getFeatureDescriptor(featureId);
      if (featureDescriptor != null) {
        usages.add(new UsageDescriptor(featureId, featureDescriptor.getUsageCount()));
      }
    }

    return usages;
  }
}
