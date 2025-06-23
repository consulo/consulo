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
package consulo.ide.impl.idea.openapi.diff.impl.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.FilePathsHelper;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Irina.Chernushina
 * @since 2011-10-17
 */
@ExtensionImpl
public class CharsetEP implements PatchEP {
  private final static Key<Map<String, String>> ourName = Key.create("Charset");
  
  private final Project myProject;
  private final String myBaseDir;

  @Inject
  public CharsetEP(Project project) {
    myProject = project;
    myBaseDir = myProject.getBaseDir().getPath();
  }

  @Nonnull
  @Override
  public String getName() {
    return "consulo.ide.impl.idea.openapi.diff.impl.patch.CharsetEP";
  }

  @Override
  public CharSequence provideContent(@Nonnull String path, CommitContext commitContext) {
    final File file = new File(myBaseDir, path);
    final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    return vf == null ? null : vf.getCharset().name();
  }

  @Override
  public void consumeContent(@Nonnull String path, @Nonnull CharSequence content, CommitContext commitContext) {
  }

  @Override
  public void consumeContentBeforePatchApplied(@Nonnull String path,
                                               @Nonnull CharSequence content,
                                               CommitContext commitContext) {
    if (commitContext == null) return;
    Map<String, String> map = commitContext.getUserData(ourName);
    if (map == null) {
      map = new HashMap<String, String>();
      commitContext.putUserData(ourName, map);
    }
    final File file = new File(myBaseDir, path);
    map.put(FilePathsHelper.convertPath(file.getPath()), content.toString());
  }
  
  public static String getCharset(final String path, final CommitContext commitContext) {
    if (commitContext == null) return null;
    final Map<String, String> userData = commitContext.getUserData(ourName);
    return userData == null ? null : userData.get(FilePathsHelper.convertPath(path));
  }
}
