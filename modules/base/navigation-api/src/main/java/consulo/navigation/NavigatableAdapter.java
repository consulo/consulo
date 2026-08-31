/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Very often both methods <code>canNavigate</code> and <code>canNavigateToSource</code>
 * return <code>true</code>. This adapter class lets focus on navigation
 * routine only.
 *
 * @author Konstantin Bulenkov
 */
@Deprecated
public abstract class NavigatableAdapter implements Navigatable {
    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return true;
    }

    @RequiredUIAccess
    public static void navigate(ComponentManager project, VirtualFile file, boolean requestFocus) {
        navigate(project, file, 0, requestFocus);
    }

    @RequiredUIAccess
    public static void navigate(ComponentManager project, VirtualFile file, int offset, boolean requestFocus) {
        OpenFileDescriptorFactory.getInstance(project).builder(file).offset(offset).build().navigate(requestFocus);
    }
}
