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
package consulo.project.ui.view.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.ui.view.tree.ProjectViewSettings;

/**
 * @author VISTALL
 * @since 2025-05-31
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectViewSharedSettings extends ProjectViewSettings {

    void setFlattenPackages(boolean flattenPackages);

    boolean isShowMembers();

    void setShowMembers(boolean showMembers);

    boolean getSortByType();

    void setSortByType(boolean sortByType);

    boolean getShowModules();

    void setShowModules(boolean showModules);

    void setShowLibraryContents(boolean showLibraryContents);

    boolean isHideEmptyPackages();

    void setHideEmptyPackages(boolean hideEmptyPackages);

    boolean isAbbreviatePackages();

    void setAbbreviatePackages(boolean abbreviatePackages);

    boolean isAutoscrollFromSource();

    void setAutoscrollFromSource(boolean autoscrollFromSource);

    boolean isAutoscrollToSource();

    void setAutoscrollToSource(boolean autoscrollToSource);

    void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop);

    boolean isFoldersAlwaysOnTop();
}
