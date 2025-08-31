/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;

import java.util.Comparator;

public class ChangesComparator implements Comparator<Change> {
  private static final ChangesComparator ourFlattenedInstance = new ChangesComparator(false);
  private static final ChangesComparator ourTreeInstance = new ChangesComparator(true);
  private final boolean myTreeCompare;

  public static ChangesComparator getInstance(boolean flattened) {
    if (flattened) {
      return ourFlattenedInstance;
    } else {
      return ourTreeInstance;
    }
  }

  private ChangesComparator(boolean treeCompare) {
    myTreeCompare = treeCompare;
  }

  @Override
  public int compare(Change o1, Change o2) {
    FilePath filePath1 = ChangesUtil.getFilePath(o1);
    FilePath filePath2 = ChangesUtil.getFilePath(o2);
    if (myTreeCompare) {
      String path1 = FileUtil.toSystemIndependentName(filePath1.getPath());
      String path2 = FileUtil.toSystemIndependentName(filePath2.getPath());
      int lastSlash1 = path1.lastIndexOf('/');
      String parentPath1 = lastSlash1 >= 0 && !filePath1.isDirectory() ? path1.substring(0, lastSlash1) : path1;
      int lastSlash2 = path2.lastIndexOf('/');
      String parentPath2 = lastSlash2 >= 0 && !filePath2.isDirectory() ? path2.substring(0, lastSlash2) : path2;
      // subdirs precede files
      if (FileUtil.isAncestor(parentPath2, parentPath1, true)) {
        return -1;
      }
      else if (FileUtil.isAncestor(parentPath1, parentPath2, true)) {
        return 1;
      }
      int compare = StringUtil.compare(parentPath1, parentPath2, !Platform.current().fs().isCaseSensitive());
      if (compare != 0) {
        return compare;
      }
    }
    return filePath1.getName().compareToIgnoreCase(filePath2.getName());
  }
}
