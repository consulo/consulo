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
package consulo.versionControlSystem.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Streams;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class VcsContextWrapper implements VcsContext {
  @Nonnull
  protected final DataContext myContext;
  protected final int myModifiers;
  @Nonnull
  private final String myPlace;
  @Nullable
  private final String myActionName;

  public VcsContextWrapper(@Nonnull DataContext context, int modifiers, @Nonnull String place, @Nullable String actionName) {
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
    return myContext.getData(Project.KEY);
  }

  @Nullable
  @Override
  public VirtualFile getSelectedFile() {
    return getSelectedFilesStream().findFirst().orElse(null);
  }

  @Nonnull
  @Override
  public VirtualFile[] getSelectedFiles() {
    VirtualFile[] fileArray = myContext.getData(VirtualFile.KEY_OF_ARRAY);
    if (fileArray != null) {
      return Stream.of(fileArray).filter(VirtualFile::isInLocalFileSystem).toArray(VirtualFile[]::new);
    }

    VirtualFile file = myContext.getData(VirtualFile.KEY);
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
    return myContext.getData(Editor.KEY);
  }

  @Override
  public Collection<VirtualFile> getSelectedFilesCollection() {
    return Arrays.asList(getSelectedFiles());
  }

  @Nullable
  @Override
  public File getSelectedIOFile() {
    File file = myContext.getData(VcsDataKeys.IO_FILE);

    return file != null ? file : ArrayUtil.getFirstElement(myContext.getData(VcsDataKeys.IO_FILE_ARRAY));
  }

  @Nullable
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

    return Streams.concat(
      path != null ? Stream.of(path) : Stream.empty(),
      Streams.stream(myContext.getData(VcsDataKeys.FILE_PATH_ARRAY)),
      getSelectedFilesStream().map(VcsUtil::getFilePath),
      Streams.stream(getSelectedIOFiles()).map(VcsUtil::getFilePath)
    );
  }

  @Nullable
  @Override
  public FilePath getSelectedFilePath() {
    return ArrayUtil.getFirstElement(getSelectedFilePaths());
  }

  @Nullable
  @Override
  public ChangeList[] getSelectedChangeLists() {
    return myContext.getData(VcsDataKeys.CHANGE_LISTS);
  }

  @Nullable
  @Override
  public Change[] getSelectedChanges() {
    return myContext.getData(VcsDataKeys.CHANGES);
  }
}
