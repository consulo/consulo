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

import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitContext;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 9/15/11
 * Time: 1:20 PM
 */
public class BaseRevisionTextPatchEP implements PatchEP {
  public final static Key<Boolean> ourPutBaseRevisionTextKey = Key.create("consulo.ide.impl.idea.openapi.diff.impl.patch.BaseRevisionTextPatchEP.ourPutBaseRevisionTextKey");
  public static final Key<List<FilePath>> ourBaseRevisionPaths = Key.create("consulo.ide.impl.idea.openapi.diff.impl.patch.BaseRevisionTextPatchEP.ourBaseRevisionPaths");
  public static final Key<Map<String, String>> ourStoredTexts = Key.create("consulo.ide.impl.idea.openapi.diff.impl.patch.BaseRevisionTextPatchEP.ourStoredTexts");
  private final static Logger LOG = Logger.getInstance(BaseRevisionTextPatchEP.class);

  private final Project myProject;
  private final ChangeListManager myChangeListManager;
  private final String myBaseDir;

  public BaseRevisionTextPatchEP(final Project project) {
    myProject = project;
    myBaseDir = myProject.getBaseDir().getPath();
    myChangeListManager = ChangeListManager.getInstance(myProject);
  }

  @Nonnull
  @Override
  public String getName() {
    return "consulo.ide.impl.idea.openapi.diff.impl.patch.BaseRevisionTextPatchEP";
  }

  @Override
  public CharSequence provideContent(@Nonnull String path, CommitContext commitContext) {
    if (commitContext == null) return null;
    if (Boolean.TRUE.equals(commitContext.getUserData(ourPutBaseRevisionTextKey))) {
      final File file = new File(myBaseDir, path);
      FilePath filePathOn = VcsContextFactory.SERVICE.getInstance().createFilePathOn(file);
      final Change change = myChangeListManager.getChange(filePathOn);
      List<FilePath> paths = commitContext.getUserData(ourBaseRevisionPaths);
      if (change == null || change.getBeforeRevision() == null || paths == null || ! paths.contains(filePathOn)) return null;

      try {
        final String content = change.getBeforeRevision().getContent();
        return content;
      }
      catch (VcsException e) {
        LOG.info(e);
      }
    } else {
      final Map<String, String> map = commitContext.getUserData(ourStoredTexts);
      if (map != null) {
        final File file = new File(myBaseDir, path);
        return map.get(file.getPath());
      }
    }
    return null;
  }

  @Override
  public void consumeContent(@Nonnull String path, @Nonnull CharSequence content, CommitContext commitContext) {
  }

  @Override
  public void consumeContentBeforePatchApplied(@Nonnull String path,
                                               @Nonnull CharSequence content,
                                               CommitContext commitContext) {
    if (commitContext == null) return;
    Map<String, String> map = commitContext.getUserData(ourStoredTexts);
    if (map == null) {
      map = new HashMap<String, String>();
      commitContext.putUserData(ourStoredTexts, map);
    }
    final File file = new File(myBaseDir, path);
    map.put(file.getPath(), content.toString());
  }
}
