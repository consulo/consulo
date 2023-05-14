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

package consulo.language.editor.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class TreeFileChooserFactory {
  public static TreeFileChooserFactory getInstance(Project project) {
    return project.getInstance(TreeFileChooserFactory.class);
  }

  @Nonnull
  public abstract TreeFileChooser createFileChooser(@Nonnull String title, @Nullable PsiFile initialFile, @Nullable FileType fileType, @Nullable Predicate<PsiFile> filter);


  @Nonnull
  public abstract TreeFileChooser createFileChooser(@Nonnull String title,
                                                    @Nullable PsiFile initialFile,
                                                    @Nullable FileType fileType,
                                                    @Nullable Predicate<PsiFile> filter,
                                                    boolean disableStructureProviders);


  @Nonnull
  public abstract TreeFileChooser createFileChooser(@Nonnull String title,
                                                    @Nullable PsiFile initialFile,
                                                    @Nullable FileType fileType,
                                                    @Nullable Predicate<PsiFile> filter,
                                                    boolean disableStructureProviders,
                                                    boolean showLibraryContents);
}
