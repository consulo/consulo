/*
 * Copyright 2013-2026 consulo.io
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
package consulo.project.impl.internal;

import consulo.application.Application;
import consulo.component.internal.ComponentBinding;
import consulo.project.ProjectManager;
import consulo.project.ProjectType;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
public class WelcomeProjectImpl extends ProjectImpl {
    private static final String WELCOME_PROJECT_NAME = "Welcome";

    private Element myStateElement;

    WelcomeProjectImpl(@Nonnull Application application,
                       @Nonnull ProjectManager manager,
                       @Nonnull ComponentBinding componentBinding) {
        super(application, manager, "", WELCOME_PROJECT_NAME, true, componentBinding);
    }

    @Nullable
    public Element getStateElement() {
        return myStateElement;
    }

    public void setStateElement(@Nullable Element stateElement) {
        myStateElement = stateElement;
    }

    @Nonnull
    @Override
    public ProjectType getProjectType() {
        return ProjectType.WELCOME;
    }
}
