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

package consulo.execution.executor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Executor {
    public static final ExtensionPointName<Executor> EP_NAME = ExtensionPointName.create(Executor.class);

    @Nonnull
    public abstract String getId();

    @Nonnull
    public abstract String getToolWindowId();

    public abstract Image getToolWindowIcon();

    @Nonnull
    public abstract Image getIcon();

    @Nullable
    public Image getDisabledIcon() {
        return null;
    }

    @Nonnull
    public abstract LocalizeValue getDescription();

    @Nonnull
    public abstract LocalizeValue getActionName();


    @Nonnull
    public abstract LocalizeValue getStartActionText();

    @Nonnull
    public abstract LocalizeValue getStartActiveText(@Nonnull String configurationName);

    @Nonnull
    public String getContextActionId() {
        return "Context" + getId();
    }

    @Nullable
    public String getHelpId() {
        return null;
    }

    /**
     * Override this method and return {@code false} to hide executor from panel
     */
    public boolean isApplicable(@Nonnull Project project) {
        return true;
    }
}
