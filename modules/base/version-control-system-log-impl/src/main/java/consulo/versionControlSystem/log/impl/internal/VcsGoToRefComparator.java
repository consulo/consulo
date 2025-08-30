/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.virtualFileSystem.VirtualFile;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsRef;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.Map;

public class VcsGoToRefComparator implements Comparator<VcsRef> {
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myProviders;

  public VcsGoToRefComparator(@Nonnull Map<VirtualFile, VcsLogProvider> providers) {
    myProviders = providers;
  }

  @Override
  public int compare(@Nonnull VcsRef ref1, @Nonnull VcsRef ref2) {
    VcsLogProvider provider1 = myProviders.get(ref1.getRoot());
    VcsLogProvider provider2 = myProviders.get(ref2.getRoot());

    if (provider1 == null) return provider2 == null ? ref1.getName().compareTo(ref2.getName()) : 1;
    if (provider2 == null) return -1;

    if (provider1 == provider2) {
      return provider1.getReferenceManager().getLabelsOrderComparator().compare(ref1, ref2);
    }

    return provider1.getSupportedVcs().getName().compareTo(provider2.getSupportedVcs().getName());
  }
}
