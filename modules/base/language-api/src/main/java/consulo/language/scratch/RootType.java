/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.scratch;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.disposer.Disposable;
import consulo.language.Language;
import consulo.language.file.FileTypeManager;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author gregsh
 * <p>
 * Created on 1/19/15
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RootType {

  public static final ExtensionPointName<RootType> ROOT_EP = ExtensionPointName.create(RootType.class);

  
  public static List<RootType> getAllRootTypes() {
    return ROOT_EP.getExtensionList();
  }

  public static @Nullable RootType forFile(@Nullable VirtualFile file) {
    return ScratchFileService.getInstance().getRootType(file);
  }

  
  public static <T extends RootType> T findByClass(Class<T> aClass) {
    return ROOT_EP.findExtensionOrFail(aClass);
  }

  private final String myId;
  private final String myDisplayName;

  protected RootType(String id, @Nullable String displayName) {
    myId = id;
    myDisplayName = displayName;
  }

  
  public final String getId() {
    return myId;
  }

  public final @Nullable String getDisplayName() {
    return myDisplayName;
  }

  public boolean isHidden() {
    return StringUtil.isEmpty(myDisplayName);
  }

  public boolean containsFile(@Nullable VirtualFile file) {
    if (file == null) return false;
    ScratchFileService service = ScratchFileService.getInstance();
    return service.getRootType(file) == this;
  }

  public @Nullable Language substituteLanguage(Project project, VirtualFile file) {
    return null;
  }

  public @Nullable Image substituteIcon(Project project, VirtualFile file) {
    if (file.isDirectory()) return null;
    Language language = substituteLanguage(project, file);
    FileType fileType = LanguageUtil.getLanguageFileType(language);
    if (fileType == null) {
      String extension = file.getExtension();
      fileType = extension == null ? null : FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence());
    }
    return fileType != null ? fileType.getIcon() : null;
  }

  public @Nullable String substituteName(Project project, VirtualFile file) {
    return null;
  }

  public VirtualFile findFile(@Nullable Project project, String pathName, ScratchFileService.Option option) throws IOException {
    return ScratchFileService.getInstance().findFile(this, pathName, option);
  }

  public boolean isIgnored(Project project, VirtualFile element) {
    return false;
  }

  public void registerTreeUpdater(Project project, Disposable disposable, Runnable onUpdate) {
  }
}
