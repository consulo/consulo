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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigable;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-10
 */
@ServiceImpl
@Singleton
public class XSourcePositionFactoryImpl implements XSourcePositionFactory {
    public static class XSourcePositionNavigable implements Navigatable {
        private final Project myProject;
        private final XSourcePosition myPosition;

        public XSourcePositionNavigable(Project project, XSourcePosition position) {
            myProject = project;
            myPosition = position;
        }

        @Override
        @RequiredUIAccess
        public void navigate(boolean requestFocus) {
            XSourcePositionImpl.createOpenFileDescriptor(myProject, myPosition).navigate(requestFocus);
        }

        @Override
        @RequiredReadAction
        public boolean canNavigate() {
            return myPosition.getFile().isValid();
        }

        @Override
        @RequiredReadAction
        public boolean canNavigateToSource() {
            return canNavigate();
        }
    }

    @Override
    public @Nullable XSourcePosition createPosition(@Nullable VirtualFile file, int line) {
        return file == null ? null : XSourcePositionImpl.create(file, line);
    }

    @Override
    public @Nullable XSourcePosition createPosition(@Nullable VirtualFile file, int line, int column) {
        return file == null ? null : XSourcePositionImpl.create(file, line, column);
    }

    @Override
    public @Nullable XSourcePosition createPositionByOffset(VirtualFile file, int offset) {
        return XSourcePositionImpl.createByOffset(file, offset);
    }

    @Override
    public @Nullable XSourcePosition createPositionByElement(PsiElement element) {
        return XSourcePositionImpl.createByElement(element);
    }

    @Override
    public Navigable createDefaultNavigable(Project project, XSourcePosition position) {
        return new XSourcePositionNavigable(project, position);
    }
}
