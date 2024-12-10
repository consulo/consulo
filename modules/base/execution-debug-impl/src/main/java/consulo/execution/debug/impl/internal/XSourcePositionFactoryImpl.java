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
package consulo.execution.debug.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-12-10
 */
@ServiceImpl
@Singleton
public class XSourcePositionFactoryImpl implements XSourcePositionFactory {
    public static class XSourcePositionNavigatable implements Navigatable {
        private final Project myProject;
        private final XSourcePosition myPosition;

        public XSourcePositionNavigatable(Project project, XSourcePosition position) {
            myProject = project;
            myPosition = position;
        }

        @Override
        public void navigate(boolean requestFocus) {
            XSourcePositionImpl.createOpenFileDescriptor(myProject, myPosition).navigate(requestFocus);
        }

        @Override
        public boolean canNavigate() {
            return myPosition.getFile().isValid();
        }

        @Override
        public boolean canNavigateToSource() {
            return canNavigate();
        }
    }

    @Override
    @Nullable
    public XSourcePosition createPosition(@Nullable VirtualFile file, int line) {
        return file == null ? null : XSourcePositionImpl.create(file, line);
    }

    @Override
    @Nullable
    public XSourcePosition createPosition(@Nullable VirtualFile file, final int line, final int column) {
        return file == null ? null : XSourcePositionImpl.create(file, line, column);
    }

    @Override
    @Nullable
    public XSourcePosition createPositionByOffset(final VirtualFile file, final int offset) {
        return XSourcePositionImpl.createByOffset(file, offset);
    }

    @Override
    @Nullable
    public XSourcePosition createPositionByElement(PsiElement element) {
        return XSourcePositionImpl.createByElement(element);
    }

    @Nonnull
    @Override
    public Navigatable createDefaultNavigatable(@Nonnull Project project, @Nonnull XSourcePosition position) {
        return new XSourcePositionNavigatable(project, position);
    }
}
