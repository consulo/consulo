/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.content.library.ui;

import consulo.application.progress.ProgressIndicator;
import consulo.content.OrderRootType;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/**
 * Base class for {@link RootDetector}'s implementations which can accept only file selected by user but not its descendants
 *
 * @author nik
 */
public abstract class RootFilter extends RootDetector {
  public RootFilter(OrderRootType rootType, boolean jarDirectory, final String presentableRootTypeName) {
    super(rootType, jarDirectory, presentableRootTypeName);
  }

  public abstract boolean isAccepted(@Nonnull VirtualFile rootCandidate, @Nonnull ProgressIndicator progressIndicator);

  @Nonnull
  @Override
  public Collection<VirtualFile> detectRoots(@Nonnull VirtualFile rootCandidate, @Nonnull ProgressIndicator progressIndicator) {
    if (isAccepted(rootCandidate, progressIndicator)) {
      return Collections.singletonList(rootCandidate);
    }
    return Collections.emptyList();
  }
}
