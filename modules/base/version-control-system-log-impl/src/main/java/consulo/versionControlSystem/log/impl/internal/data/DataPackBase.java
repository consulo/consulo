/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.virtualFileSystem.VirtualFile;
import consulo.versionControlSystem.log.VcsLogProvider;
import jakarta.annotation.Nonnull;

import java.util.Map;

class DataPackBase {
  @Nonnull
  protected final Map<VirtualFile, VcsLogProvider> myLogProviders;
  @Nonnull
  protected final RefsModel myRefsModel;
  protected final boolean myIsFull;

  DataPackBase(@Nonnull Map<VirtualFile, VcsLogProvider> providers, @Nonnull RefsModel refsModel, boolean isFull) {
    myLogProviders = providers;
    myRefsModel = refsModel;
    myIsFull = isFull;
  }

  @Nonnull
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myLogProviders;
  }

  @Nonnull
  public RefsModel getRefsModel() {
    return myRefsModel;
  }

  public boolean isFull() {
    return myIsFull;
  }
}
