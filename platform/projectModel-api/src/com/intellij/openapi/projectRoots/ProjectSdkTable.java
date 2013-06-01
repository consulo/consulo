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
package com.intellij.openapi.projectRoots;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;
import java.util.List;

public abstract class ProjectSdkTable {
  public static Topic<Listener> SDK_TABLE_TOPIC = Topic.create("Project SDK table", Listener.class);

  @NotNull
  public static ProjectSdkTable getInstance() {
    return ServiceManager.getService(ProjectSdkTable.class);
  }

  @Nullable
  public abstract Sdk findSdk(String name);

  @Nullable
  public abstract Sdk findSdk(String name, String type);

  public abstract Sdk[] getAllSdks();

  public abstract List<Sdk> getSdksOfType(SdkTypeId type);

  @Nullable
  public Sdk findMostRecentSdkOfType(final SdkTypeId type) {
    return findMostRecentSdk(new Condition<Sdk>() {
      @Override
      public boolean value(Sdk sdk) {
        return sdk.getSdkType() == type;
      }
    });
  }

  @Nullable
  public Sdk findMostRecentSdk(Condition<Sdk> condition) {
    Sdk found = null;
    for (Sdk each : getAllSdks()) {
      if (!condition.value(each)) continue;
      if (found == null) {
        found = each;
        continue;
      }
      if (Comparing.compare(each.getVersionString(), found.getVersionString()) > 0) found = each;
    }
    return found;
  }

  public abstract void addSdk(Sdk sdk);

  public abstract void removeSdk(Sdk sdk);

  public abstract void updateSdk(Sdk originalSdk, Sdk modifiedSdk);

  public interface Listener extends EventListener {
    void sdkAdded(Sdk sdk);

    void sdkRemoved(Sdk sdk);

    void sdkNameChanged(Sdk sdk, String previousName);
  }

  /**
   * @deprecated use {@link ProjectSdkTable#SDK_TABLE_TOPIC} instead
   */
  public abstract void addListener(Listener listener);

  /**
   * @deprecated use {@link ProjectSdkTable#SDK_TABLE_TOPIC} instead
   */
  public abstract void removeListener(Listener listener);

  public abstract SdkTypeId getDefaultSdkType();

  public abstract SdkTypeId getSdkTypeByName(String name);

  public abstract Sdk createSdk(final String name, final SdkTypeId sdkType);
}
