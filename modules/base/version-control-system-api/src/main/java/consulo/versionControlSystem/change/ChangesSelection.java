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
package consulo.versionControlSystem.change;

import jakarta.annotation.Nonnull;
import java.util.List;

public class ChangesSelection {
  @Nonnull
  private final List<Change> myChanges;
  private final int myIndex;

  public ChangesSelection(@Nonnull List<Change> changes, int index) {
    myChanges = changes;
    myIndex = index;
  }

  @Nonnull
  public List<Change> getChanges() {
    return myChanges;
  }

  public int getIndex() {
    return myIndex;
  }
}
