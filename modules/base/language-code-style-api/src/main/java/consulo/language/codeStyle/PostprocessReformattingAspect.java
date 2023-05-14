/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.file.FileViewProvider;
import consulo.language.pom.PomModelAspect;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 23-Mar-22
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public interface PostprocessReformattingAspect extends PomModelAspect {
  public static PostprocessReformattingAspect getInstance(Project project) {
    return project.getInstance(PostprocessReformattingAspect.class);
  }

  void doPostponedFormatting();

  void doPostponedFormatting(@Nonnull FileViewProvider viewProvider);

  void disablePostprocessFormattingInside(@Nonnull final Runnable runnable);

  <T> T disablePostprocessFormattingInside(@Nonnull Supplier<T> computable);

  void postponeFormattingInside(@Nonnull final Runnable runnable);

  <T> T postponeFormattingInside(@Nonnull Supplier<T> computable);

  boolean isViewProviderLocked(@Nonnull FileViewProvider fileViewProvider);
}
