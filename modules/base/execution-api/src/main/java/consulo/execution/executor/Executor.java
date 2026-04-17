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
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

/**
 * @author spleaner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Executor {
    
    public abstract String getId();

    
    public abstract String getToolWindowId();

    public abstract Image getToolWindowIcon();

    public Image getToolWindowIconIfRunning() {
        return ImageEffects.layered(getToolWindowIcon(), PlatformIconGroup.greenbadge());
    }

    public abstract Image getIcon();

    public @Nullable Image getDisabledIcon() {
        return null;
    }

    public abstract LocalizeValue getDescription();

    public abstract LocalizeValue getActionName();

    public abstract LocalizeValue getStartActionText();

    public abstract LocalizeValue getStartActiveText(String configurationName);

    public String getContextActionId() {
        return "Context" + getId();
    }

    public @Nullable String getHelpId() {
        return null;
    }

    /**
     * Override this method and return {@code false} to hide executor from panel
     */
    public boolean isApplicable(Project project) {
        return true;
    }
}
