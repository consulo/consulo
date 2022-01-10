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

package com.intellij.internal.statistic.persistence;

import com.intellij.internal.statistic.StatisticsUploadAssistant;
import com.intellij.internal.statistic.beans.PatchedUsage;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.util.Pair;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BasicSentUsagesPersistenceComponent extends SentUsagesPersistence {

  protected Map<String, Set<UsageDescriptor>> mySentDescriptors = new HashMap<>();

  private long mySentTime = 0;

  @Override
  public boolean isAllowed() {
    return true;
  }

  @Override
  public long getLastTimeSent() {
    return mySentTime;
  }

  public void setSentTime(long time) {
    mySentTime = time;
  }

  @Override
  public void persistPatch(@Nonnull Map<String, Set<PatchedUsage>> patchedDescriptorMap) {
    for (Map.Entry<String, Set<PatchedUsage>> entry : patchedDescriptorMap.entrySet()) {
      final String groupDescriptor = entry.getKey();
      for (PatchedUsage patchedUsage : entry.getValue()) {
        UsageDescriptor usageDescriptor = StatisticsUploadAssistant.findDescriptor(mySentDescriptors, Pair.create(groupDescriptor, patchedUsage.getKey()));
        if (usageDescriptor != null) {
          usageDescriptor.setValue(usageDescriptor.getValue() + patchedUsage.getDelta());
        }
        else {
          if (!mySentDescriptors.containsKey(groupDescriptor)) {
            mySentDescriptors.put(groupDescriptor, new HashSet<>());
          }
          mySentDescriptors.get(groupDescriptor).add(new UsageDescriptor(patchedUsage.getKey(), patchedUsage.getValue()));
        }
      }
    }

    setSentTime(System.currentTimeMillis());
  }


  @Override
  @Nonnull
  public Map<String, Set<UsageDescriptor>> getSentUsages() {
    return mySentDescriptors;
  }
}
