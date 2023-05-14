/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.log.RefGroup;
import consulo.versionControlSystem.log.VcsRef;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * {@link RefGroup} containing only one {@link VcsRef}.
 */
public class SingletonRefGroup implements RefGroup {
  private final VcsRef myRef;

  public SingletonRefGroup(VcsRef ref) {
    myRef = ref;
  }

  @Override
  public boolean isExpanded() {
    return false;
  }

  @Nonnull
  @Override
  public String getName() {
    return myRef.getName();
  }

  @Nonnull
  @Override
  public List<VcsRef> getRefs() {
    return Collections.singletonList(myRef);
  }

  @Nonnull
  @Override
  public List<Color> getColors() {
    return Collections.singletonList(myRef.getType().getBackgroundColor());
  }
}
