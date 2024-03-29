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
package consulo.ide.impl.idea.openapi.roots.libraries.ui.impl;

import consulo.content.library.LibraryRootType;
import consulo.content.library.ui.DetectedLibraryRoot;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.ArrayUtil;

import java.util.Arrays;
import java.util.Map;

/**
 * @author nik
 */
class SuggestedChildRootInfo {
  private final VirtualFile myRootCandidate;
  private final DetectedLibraryRoot myDetectedRoot;
  private final Map<LibraryRootType, String> myRootTypeNames;
  private LibraryRootType mySelectedRootType;

  SuggestedChildRootInfo(VirtualFile rootCandidate, DetectedLibraryRoot detectedRoot, Map<LibraryRootType, String> rootTypeNames) {
    myRootCandidate = rootCandidate;
    myDetectedRoot = detectedRoot;
    myRootTypeNames = rootTypeNames;
    mySelectedRootType = detectedRoot.getTypes().get(0);
  }

  public VirtualFile getRootCandidate() {
    return myRootCandidate;
  }

  public DetectedLibraryRoot getDetectedRoot() {
    return myDetectedRoot;
  }

  public String getRootTypeName(LibraryRootType type) {
    return myRootTypeNames.get(type);
  }

  public LibraryRootType getSelectedRootType() {
    return mySelectedRootType;
  }

  public void setSelectedRootType(String selectedRootType) {
    for (LibraryRootType type : myDetectedRoot.getTypes()) {
      if (getRootTypeName(type).equals(selectedRootType)) {
        mySelectedRootType = type;
        break;
      }
    }
  }

  public String[] getRootTypeNames() {
    final String[] types = ArrayUtil.toStringArray(myRootTypeNames.values());
    Arrays.sort(types, String.CASE_INSENSITIVE_ORDER);
    return types;
  }
}
