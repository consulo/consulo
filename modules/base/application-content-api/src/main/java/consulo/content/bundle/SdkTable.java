/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.content.bundle;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class SdkTable implements BundleHolder {
  
  @Deprecated
  @DeprecationInfo("Use constructor injecting")
  public static SdkTable getInstance() {
    return Application.get().getInstance(SdkTable.class);
  }

  public abstract @Nullable Sdk findSdk(String name);

  
  public abstract Sdk[] getAllSdks();

  
  @Override
  public Sdk[] getBundles() {
    return getAllSdks();
  }

  public abstract List<Sdk> getSdksOfType(SdkTypeId type);

  public @Nullable Sdk findMostRecentSdkOfType(SdkTypeId type) {
    return findMostRecentSdk(sdk -> sdk.getSdkType() == type);
  }

  public @Nullable Sdk findMostRecentSdk(Predicate<Sdk> condition) {
    Sdk found = null;
    for (Sdk each : getAllSdks()) {
      if (!condition.test(each)) continue;
      if (found == null) {
        found = each;
        continue;
      }
      if (compare(each.getVersionString(), found.getVersionString()) > 0) found = each;
    }
    return found;
  }

  private static int compare(@Nullable String o1, @Nullable String o2) {
    if (Objects.equals(o1, o2)) return 0;
    if (o1 == null) return -1;
    if (o2 == null) return 1;
    return o1.compareTo(o2);
  }

  @RequiredWriteAction
  public abstract void addSdk(Sdk sdk);

  @RequiredWriteAction
  public abstract void removeSdk(Sdk sdk);

  @RequiredWriteAction
  public abstract void updateSdk(Sdk originalSdk, Sdk modifiedSdk);

  public abstract SdkTypeId getDefaultSdkType();

  public abstract SdkTypeId getSdkTypeByName(String name);

  public abstract @Nullable Sdk findPredefinedSdkByType(SdkTypeId sdkType);

  /**
   * Create sdk with target type, but not add to table. use {@link #addSdk(Sdk)} for adding
   */
  
  @Deprecated
  @DeprecationInfo("Prefer createSdk(Path,String,SdkTypeId)")
  public abstract Sdk createSdk(String name, SdkTypeId sdkType);

  /**
   * Create sdk with target type, but not add to table. use {@link #addSdk(Sdk)} for adding
   */
  
  public abstract Sdk createSdk(Path homePath, String name, SdkTypeId sdkType);
}
