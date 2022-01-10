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
package consulo.bundle;

import com.intellij.internal.statistic.CollectUsagesException;
import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-07
 */
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
      if(sdk.isPredefined()) {
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
