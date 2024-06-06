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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.util.lang.Comparing;
import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import jakarta.annotation.Nonnull;

public class TextRevisionNumber implements ShortVcsRevisionNumber {
  @Nonnull
  private final String myFullRevisionNumber;
  @Nonnull
  private final String myShortRevisionNumber;

  public TextRevisionNumber(@Nonnull String fullRevisionNumber) {
    this(fullRevisionNumber, fullRevisionNumber.substring(0, Math.min(7, fullRevisionNumber.length())));
  }

  public TextRevisionNumber(@Nonnull String fullRevisionNumber, @Nonnull String shortRevisionNumber) {
    myFullRevisionNumber = fullRevisionNumber;
    myShortRevisionNumber = shortRevisionNumber;
  }

  @Override
  public String asString() {
    return myFullRevisionNumber;
  }

  @Override
  public int compareTo(@Nonnull final VcsRevisionNumber o) {
    return Comparing.compare(myFullRevisionNumber, ((TextRevisionNumber) o).myFullRevisionNumber);
  }

  @Override
  public String toShortString() {
    return myShortRevisionNumber;
  }

}
