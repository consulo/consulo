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
package consulo.language.copyright;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.copyright.config.CopyrightFileConfig;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.ui.TemplateCommentPanel;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class UpdateCopyrightsProvider<T extends CopyrightFileConfig> {
  private static final ExtensionPointCacheKey<UpdateCopyrightsProvider, Map<FileType, UpdateCopyrightsProvider>> KEY =
          ExtensionPointCacheKey.groupBy("UpdateCopyrightsProvider", UpdateCopyrightsProvider::getFileType);

  @Nullable
  public static UpdateCopyrightsProvider forFileType(FileType fileType) {
    ExtensionPoint<UpdateCopyrightsProvider> extensionPoint = Application.get().getExtensionPoint(UpdateCopyrightsProvider.class);
    Map<FileType, UpdateCopyrightsProvider> map = extensionPoint.getOrBuildCache(KEY);
    return map.get(fileType);
  }

  public static boolean hasExtension(@Nonnull PsiFile psiFile) {
    VirtualFile virtualFile = psiFile.getVirtualFile();
    return virtualFile != null && hasExtension(virtualFile);
  }

  public static boolean hasExtension(@Nonnull VirtualFile virtualFile) {
    return forFileType(virtualFile.getFileType()) != null;
  }

  @Nonnull
  public abstract FileType getFileType();

  @Nonnull
  public abstract UpdatePsiFileCopyright<T> createInstance(@Nonnull PsiFile file, @Nonnull CopyrightProfile copyrightProfile);

  @Nonnull
  public abstract T createDefaultOptions();

  @Nonnull
  public abstract TemplateCommentPanel createConfigurable(@Nonnull Project project, @Nonnull TemplateCommentPanel parentPane, @Nonnull FileType fileType);

  public boolean isAllowSeparator() {
    return true;
  }
}
