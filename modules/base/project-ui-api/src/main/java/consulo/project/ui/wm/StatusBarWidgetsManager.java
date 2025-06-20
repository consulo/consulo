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
package consulo.project.ui.wm;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.ModificationTracker;
import consulo.project.Project;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04.05.2024
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface StatusBarWidgetsManager extends ModificationTracker {
    @Nonnull
    public static StatusBarWidgetsManager getInstance(@Nonnull Project project) {
        return project.getInstance(StatusBarWidgetsManager.class);
    }

    void updateWidget(@Nonnull Class<? extends StatusBarWidgetFactory> factoryExtension, @Nonnull UIAccess uiAccess);

    void updateWidget(@Nonnull StatusBarWidgetFactory factory, @Nonnull UIAccess uiAccess);

    void updateAllWidgets(@Nonnull UIAccess uiAccess);
}
