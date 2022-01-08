/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.usages;

import com.intellij.icons.AllIcons;
import com.intellij.usageView.UsageInfo;
import javax.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 17, 2005
 */
public class ReadWriteAccessUsageInfo2UsageAdapter extends UsageInfo2UsageAdapter implements ReadWriteAccessUsage{
  private final boolean myAccessedForReading;
  private final boolean myAccessedForWriting;

  public ReadWriteAccessUsageInfo2UsageAdapter(@Nonnull UsageInfo usageInfo, final boolean accessedForReading, final boolean accessedForWriting) {
    super(usageInfo);
    myAccessedForReading = accessedForReading;
    myAccessedForWriting = accessedForWriting;
    if (myAccessedForReading && myAccessedForWriting) {
      myIcon = AllIcons.Nodes.Rw_access;
    }
    else if (myAccessedForWriting) {
      myIcon = AllIcons.Nodes.Write_access;           // If icon is changed, don't forget to change UTCompositeUsageNode.getIcon();
    }
    else if (myAccessedForReading){
      myIcon = AllIcons.Nodes.Read_access;            // If icon is changed, don't forget to change UTCompositeUsageNode.getIcon();
    }
  }

  @Override
  public boolean isAccessedForWriting() {
    return myAccessedForWriting;
  }

  @Override
  public boolean isAccessedForReading() {
    return myAccessedForReading;
  }
}
