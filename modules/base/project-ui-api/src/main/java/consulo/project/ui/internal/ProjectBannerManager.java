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
package consulo.project.ui.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

/**
 * Places the banners of {@link ProjectBannerProvider} on top of the list of running tasks of the status bar.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectBannerManager {
    static ProjectBannerManager getInstance(Project project) {
        return project.getInstance(ProjectBannerManager.class);
    }

    /**
     * Asks every provider again whether its banner is up. Safe from any thread.
     */
    void update();

    /**
     * Shows {@code message} in the banner of {@code id}: the list of running tasks opens without taking the focus, and
     * the banner is in it even if it was closed or dismissed, for as long as the state lasts. Safe from any thread.
     */
    void reveal(String id, LocalizeValue message);
}
