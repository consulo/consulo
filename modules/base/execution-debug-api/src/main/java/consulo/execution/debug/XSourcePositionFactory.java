/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.debug;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-10
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface XSourcePositionFactory {
    /**
     * Create {@link XSourcePosition} instance by line number
     *
     * @param file file
     * @param line 0-based line number
     * @return source position
     */
    @Nullable
    XSourcePosition createPosition(@Nullable VirtualFile file, int line);

    /**
     * Create {@link XSourcePosition} instance by line and column number
     *
     * @param file   file
     * @param line   0-based line number
     * @param column 0-based column number
     * @return source position
     */
    @Nullable
    XSourcePosition createPosition(@Nullable VirtualFile file, int line, int column);

    /**
     * Create {@link XSourcePosition} instance by line number
     *
     * @param file   file
     * @param offset offset from the beginning of file
     * @return source position
     */
    @Nullable
    XSourcePosition createPositionByOffset(@Nullable VirtualFile file, int offset);

    @Nullable
    XSourcePosition createPositionByElement(@Nullable PsiElement element);

    @Nonnull
    @UsedInPlugin
    Navigatable createDefaultNavigatable(@Nonnull Project project, @Nonnull XSourcePosition position);
}
