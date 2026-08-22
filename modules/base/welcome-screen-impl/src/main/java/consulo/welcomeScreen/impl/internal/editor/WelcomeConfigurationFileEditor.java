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
package consulo.welcomeScreen.impl.internal.editor;

import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-17
 */
public class WelcomeConfigurationFileEditor extends ConfigurationFileEditor {
    private @Nullable DockLayout myComponent;

    public WelcomeConfigurationFileEditor(Project project, VirtualFile virtualFile) {
        super(project, virtualFile);
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        if (myComponent == null) {
            myComponent = DockLayout.create();
        }
        return myComponent;
    }

    @Override
    public void dispose() {

    }
}
