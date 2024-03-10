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
package consulo.versionControlSystem;

import consulo.application.ApplicationManager;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Supplier;

public class VcsDescriptor implements Comparable<VcsDescriptor> {
  private final String myId;
  private final boolean myCrawlUpToCheckUnderVcs;
  private final LocalizeValue myDisplayName;
  private final String myAdministrativePattern;
  private boolean myIsNone;

  public VcsDescriptor(String administrativePattern, @Nonnull LocalizeValue displayName, String id, boolean crawlUpToCheckUnderVcs) {
    myAdministrativePattern = administrativePattern;
    myDisplayName = displayName;
    myId = id;
    myCrawlUpToCheckUnderVcs = crawlUpToCheckUnderVcs;
  }

  public boolean probablyUnderVcs(final VirtualFile file) {
    if (file == null || (!file.isDirectory()) || (!file.isValid())) return false;
    if (myAdministrativePattern == null) return false;
    return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>)() -> {
      if (checkFileForBeingAdministrative(file)) return true;
      if (myCrawlUpToCheckUnderVcs) {
        VirtualFile current = file.getParent();
        while (current != null) {
          if (checkFileForBeingAdministrative(current)) return true;
          current = current.getParent();
        }
      }
      return false;
    });
  }

  private boolean checkFileForBeingAdministrative(final VirtualFile file) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>)() -> {
      final String[] patterns = myAdministrativePattern.split(",");
      for (String pattern : patterns) {
        final VirtualFile child = file.findChild(pattern.trim());
        if (child != null) return true;
      }
      return false;
    });
  }

  @Nonnull
  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  public String getId() {
    return myId;
  }

  @Override
  public int compareTo(VcsDescriptor o) {
    return Comparing.compare(myDisplayName, o.myDisplayName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VcsDescriptor that = (VcsDescriptor)o;

    if (myId != null ? !myId.equals(that.myId) : that.myId != null) return false;

    return true;
  }

  public boolean isNone() {
    return myIsNone;
  }

  public static VcsDescriptor createFictive() {
    final VcsDescriptor vcsDescriptor = new VcsDescriptor(null, VcsLocalize.noneVcsPresentation(), null, false);
    vcsDescriptor.myIsNone = true;
    return vcsDescriptor;
  }

  @Override
  public int hashCode() {
    return myId != null ? myId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return Objects.toString(myId, "<none>");
  }
}
