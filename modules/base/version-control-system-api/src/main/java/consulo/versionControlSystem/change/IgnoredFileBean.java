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
package consulo.versionControlSystem.change;

import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.PatternUtil;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NullVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import org.jspecify.annotations.Nullable;
import java.io.File;
import java.util.regex.Matcher;

/**
 * @author yole
 * @since 2006-12-20
 */
public class IgnoredFileBean implements IgnoredFileDescriptor {
  private final String myPath;
  private final String myFilenameIfFile;
  private final String myMask;
  private final Matcher myMatcher;
  private final IgnoreSettingsType myType;
  private final Project myProject;

  IgnoredFileBean(String path, IgnoreSettingsType type, Project project) {
    myPath = path;
    myType = type;
    if (IgnoreSettingsType.FILE.equals(type)) {
      myFilenameIfFile = new File(path).getName();
    } else {
      myFilenameIfFile = null;
    }
    myProject = project;
    myMask = null;
    myMatcher = null;
  }

  public Project getProject() {
    return myProject;
  }

  IgnoredFileBean(String mask) {
    myType = IgnoreSettingsType.MASK;
    myMask = mask;
    if (mask == null) {
      myMatcher = null;
    }
    else {
      myMatcher = PatternUtil.fromMask(mask).matcher("");
    }
    myPath = null;
    myFilenameIfFile = null;
    myProject = null;
  }

  public @Nullable String getPath() {
    return myPath;
  }

  public @Nullable String getMask() {
    return myMask;
  }

  public IgnoreSettingsType getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IgnoredFileBean that = (IgnoredFileBean)o;

    if (myPath != null ? !myPath.equals(that.myPath) : that.myPath != null) return false;
    if (myMask != null ? !myMask.equals(that.myMask) : that.myMask != null) return false;
    if (myType != that.myType) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myPath != null ? myPath.hashCode() : 0;
    result = 31 * result + (myMask != null ? myMask.hashCode() : 0);
    result = 31 * result + myType.hashCode();
    return result;
  }

  @Override
  public boolean matchesFile(FilePath filePath) {
    // Fallback for non-existing files
    if (myType == IgnoreSettingsType.MASK) {
      synchronized (myMatcher) {
        myMatcher.reset(filePath.getName());
        return myMatcher.matches();
      }
    }
    else if (myType == IgnoreSettingsType.FILE) {
      if (!myFilenameIfFile.equals(filePath.getName())) return false;
      return myPath != null && FileUtil.pathsEqual(myPath, filePath.getPath());
    }
    else {
      // UNDER_DIR
      return myPath != null && FileUtil.isAncestor(myPath, filePath.getPath(), false);
    }
  }
}
