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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static consulo.ide.impl.idea.util.containers.UtilKt.concat;
import static consulo.ide.impl.idea.util.containers.UtilKt.stream;

public class VcsContextWrapper implements VcsContext {
  @Nonnull
  protected final DataContext myContext;
  protected final int myModifiers;
  @Nonnull
  private final String myPlace;
  @jakarta.annotation.Nullable
  private final String myActionName;

  public VcsContextWrapper(@Nonnull DataContext context, int modifiers, @Nonnull String place, @jakarta.annotation.Nullable String actionName) {
    myContext = context;
    myModifiers = modifiers;
    myPlace = place;
    myActionName = actionName;
  }

  @Nonnull
  @Override
  public String getPlace() {
    return myPlace;
  }

  @Nullable
  @Override
  public String getActionName() {
    return myActionName;
  }

  @Nonnull
  public static VcsContext createCachedInstanceOn(@Nonnull AnActionEvent event) {
    return new CachedVcsContext(createInstanceOn(event));
  }

  @Nonnull
  public static VcsContextWrapper createInstanceOn(@Nonnull AnActionEvent event) {
    return new VcsContextWrapper(event.getDataContext(), event.getModifiers(), event.getPlace(), event.getPresentation().getText());
  }

  @Nullable
  @Override
  public Project getProject() {
    return myContext.getData(CommonDataKeys.PROJECT);
  }

  @jakarta.annotation.Nullable
  @Override
  public VirtualFile getSelectedFile() {
    return getSelectedFilesStream().findFirst().orElse(null);
  }

  @Nonnull
  @Override
  public VirtualFile[] getSelectedFiles() {
    VirtualFile[] fileArray = myContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (fileArray != null) {
      return Stream.of(fileArray).filter(VirtualFile::isInLocalFileSystem).toArray(VirtualFile[]::new);
    }

    VirtualFile file = myContext.getData(CommonDataKeys.VIRTUAL_FILE);
    return file != null && file.isInLocalFileSystem() ? new VirtualFile[]{file} : VirtualFile.EMPTY_ARRAY;
  }

  @Nonnull
  @Override
  public Stream<VirtualFile> getSelectedFilesStream() {
    Stream<VirtualFile> result = myContext.getData(VcsDataKeys.VIRTUAL_FILE_STREAM);

    return result != null ? result.filter(VirtualFile::isInLocalFileSystem) : VcsContext.super.getSelectedFilesStream();
  }

  @Override
  public Editor getEditor() {
    return myContext.getData(CommonDataKeys.EDITOR);
  }

  @Override
  public Collection<VirtualFile> getSelectedFilesCollection() {
    return Arrays.asList(getSelectedFiles());
  }

  @jakarta.annotation.Nullable
  @Override
  public File getSelectedIOFile() {
    File file = myContext.getData(VcsDataKeys.IO_FILE);

    return file != null ? file : ArrayUtil.getFirstElement(myContext.getData(VcsDataKeys.IO_FILE_ARRAY));
  }

  @jakarta.annotation.Nullable
  @Override
  public File[] getSelectedIOFiles() {
    File[] files = myContext.getData(VcsDataKeys.IO_FILE_ARRAY);
    if (!ArrayUtil.isEmpty(files)) return files;

    File file = myContext.getData(VcsDataKeys.IO_FILE);
    return file != null ? new File[]{file} : null;
  }

  @Override
  public int getModifiers() {
    return myModifiers;
  }

  @Override
  public Refreshable getRefreshableDialog() {
    return myContext.getData(Refreshable.PANEL_KEY);
  }

  @Nonnull
  @Override
  public FilePath[] getSelectedFilePaths() {
    return getSelectedFilePathsStream().toArray(FilePath[]::new);
  }

  @Nonnull
  @Override
  public Stream<FilePath> getSelectedFilePathsStream() {
    FilePath path = myContext.getData(VcsDataKeys.FILE_PATH);

    return concat(
            path != null ? Stream.of(path) : Stream.empty(),
            stream(myContext.getData(VcsDataKeys.FILE_PATH_ARRAY)),
            getSelectedFilesStream().map(VcsUtil::getFilePath),
            stream(getSelectedIOFiles()).map(VcsUtil::getFilePath)
    );
  }

  @jakarta.annotation.Nullable
  @Override
  public FilePath getSelectedFilePath() {
    return ArrayUtil.getFirstElement(getSelectedFilePaths());
  }

  @jakarta.annotation.Nullable
  @Override
  public ChangeList[] getSelectedChangeLists() {
    return myContext.getData(VcsDataKeys.CHANGE_LISTS);
  }

  @jakarta.annotation.Nullable
  @Override
  public Change[] getSelectedChanges() {
    return myContext.getData(VcsDataKeys.CHANGES);
  }
}
