/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.diagram;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.diagram.GraphBuilder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageGraphProvider<V> {
    @Nonnull
    String getId();

    @Nonnull
    String getName(@Nonnull V element);

    @Nonnull
    @RequiredReadAction
    String getURL(@Nonnull V element);

    @Nullable
    @RequiredReadAction
    PsiElement restoreFromURL(@Nonnull Project project, @Nonnull String url);

    boolean isSupported(@Nonnull PsiElement element);

    @Nonnull
    @RequiredReadAction
    GraphBuilder createBuilder(@Nonnull V element);
}
