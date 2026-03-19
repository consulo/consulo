/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.persist.scheme;

import org.jspecify.annotations.Nullable;

public final class ExternalInfo {
  // we keep it to detect rename
  private @Nullable String myPreviouslySavedName = null;
  private @Nullable String myCurrentFileName = null;

  private int myContentHash;

  private boolean myRemote;

  @Deprecated
  @SuppressWarnings({"UnusedParameters", "unused"})
  public void setIsImported(boolean isImported) {
  }

  @Deprecated
  @SuppressWarnings("unused")
  public void setOriginalPath(String originalPath) {
  }

  @Deprecated
  @SuppressWarnings("unused")
  public boolean isIsImported() {
    return false;
  }

  @Deprecated
  @Nullable
  @SuppressWarnings("unused")
  public String getOriginalPath() {
    return null;
  }

  public @Nullable String getCurrentFileName() {
    return myCurrentFileName;
  }

  public void setCurrentFileName(@Nullable String currentFileName) {
    myCurrentFileName = currentFileName;
  }

  public void copy(ExternalInfo externalInfo) {
    myCurrentFileName = externalInfo.myCurrentFileName;
  }

  public @Nullable String getPreviouslySavedName() {
    return myPreviouslySavedName;
  }

  public void setPreviouslySavedName(String previouslySavedName) {
    myPreviouslySavedName = previouslySavedName;
  }

  public int getHash() {
    return myContentHash;
  }

  public void setHash(int newHash) {
    myContentHash = newHash;
  }

  public boolean isRemote() {
    return myRemote;
  }

  public void markRemote() {
    myRemote = true;
  }

  @Override
  public String toString() {
    return "file: " + myCurrentFileName + (myRemote ? ", remote" : "");
  }
}
