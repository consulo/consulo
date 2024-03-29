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
package consulo.versionControlSystem.change.commited;

import consulo.versionControlSystem.RepositoryLocation;
import consulo.versionControlSystem.VcsException;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class RepositoryLocationGroup implements RepositoryLocation {
  private final String myPresentableString;
  private final List<RepositoryLocation> myLocations;

  public RepositoryLocationGroup(final String presentableString) {
    myPresentableString = presentableString;
    myLocations = new ArrayList<RepositoryLocation>();
  }

  @Override
  public String toPresentableString() {
    return myPresentableString;
  }

  public void add(@Nonnull final RepositoryLocation location) {
    for (int i = 0; i < myLocations.size(); i++) {
      final RepositoryLocation t = myLocations.get(i);
      if (t.getKey().compareTo(location.getKey()) >= 0) {
        myLocations.add(i, location);
        return;
      }
    }
    myLocations.add(location);
  }

  @Override
  public String getKey() {
    final StringBuilder sb = new StringBuilder(myPresentableString);
    // they are ordered
    for (RepositoryLocation location : myLocations) {
      sb.append(location.getKey());
    }
    return sb.toString();
  }

  @Override
  public void onBeforeBatch() throws VcsException {
  }

  @Override
  public void onAfterBatch() {
  }

  public List<RepositoryLocation> getLocations() {
    return myLocations;
  }
}
