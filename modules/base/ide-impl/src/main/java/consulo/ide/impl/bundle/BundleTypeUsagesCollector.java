/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.bundle;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsagesCollector;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-07
 */
@ExtensionImpl
public class BundleTypeUsagesCollector extends UsagesCollector {
  private SdkTable mySdkTable;

  @Inject
  public BundleTypeUsagesCollector(SdkTable sdkTable) {
    mySdkTable = sdkTable;
  }

  @Nonnull
  @Override
  public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
    Map<String, UsageDescriptor> usages = new LinkedHashMap<>();
    for (Sdk sdk : mySdkTable.getBundles()) {
      String id = sdk.getSdkType().getId();
      if (sdk.isPredefined()) {
        id = "predefined." + id;
      }

      UsageDescriptor descriptor = usages.computeIfAbsent(id, k -> new UsageDescriptor(k, 0));
      descriptor.setValue(descriptor.getValue() + 1);
    }
    return new LinkedHashSet<>(usages.values());
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:bundle.type";
  }
}
