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
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2024-05-04
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface StatusBarWidgetsManager extends ModificationTracker {
    public static StatusBarWidgetsManager getInstance(Project project) {
        return project.getInstance(StatusBarWidgetsManager.class);
    }

    void updateWidget(Class<? extends StatusBarWidgetFactory> factoryExtension, UIAccess uiAccess);

    void updateWidget(StatusBarWidgetFactory factory, UIAccess uiAccess);

    default void updateAllWidgets(UIAccess uiAccess) {
        updateAllWidgets(null, uiAccess);
    }

    void updateAllWidgets(@Nullable IdeFrame frame, UIAccess uiAccess);
}
