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

import consulo.component.extension.ExtensionPoint;
import consulo.language.file.FileViewProvider;
import consulo.language.pom.PomModelAspect;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2022-03-23
 */
public interface PostprocessReformattingAspect extends PomModelAspect {
    @Nonnull
    static PostprocessReformattingAspect getInstance(Project project) {
        ExtensionPoint<PomModelAspect> point = project.getExtensionPoint(PomModelAspect.class);
        return point.findExtensionOrFail(PostprocessReformattingAspect.class);
    }

    void doPostponedFormatting();

    void doPostponedFormatting(@Nonnull FileViewProvider viewProvider);

    void disablePostprocessFormattingInside(@Nonnull Runnable runnable);

    <T> T disablePostprocessFormattingInside(@Nonnull Supplier<T> computable);

    void postponeFormattingInside(@Nonnull Runnable runnable);

    <T> T postponeFormattingInside(@Nonnull Supplier<T> computable);

    boolean isViewProviderLocked(@Nonnull FileViewProvider fileViewProvider);
}
