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

package com.intellij.openapi.module.impl.scopes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.SourcesOrderRootType;

/**
 * @author max
 */
public class SdkScope extends LibraryScopeBase {
  private final String mySdkName;

  public SdkScope(Project project, ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
    super(project, sdkOrderEntry.getFiles(BinariesOrderRootType.getInstance()), sdkOrderEntry.getFiles(SourcesOrderRootType.getInstance()));
    mySdkName = sdkOrderEntry.getSdkName();
  }

  @Override
  public int hashCode() {
    return mySdkName.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (object == null) return false;
    if (object.getClass() != SdkScope.class) return false;

    final SdkScope that = (SdkScope)object;
    return that.mySdkName.equals(mySdkName);
  }
}
