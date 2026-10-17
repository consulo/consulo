/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.versionControlSystem.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * @author irengrig
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ChangesViewContentI {
    String LOCAL_CHANGES = "Local Changes";
    String REPOSITORY = "Repository";
    String INCOMING = "Incoming";
    String SHELF = "Shelf";

    Key<Boolean> IN_COMMIT_TOOL_WINDOW_KEY = Key.create("ChangesViewContentI.IN_COMMIT_TOOL_WINDOW_KEY");

    Key<String> CONTENT_ID_KEY = Key.create("ChangesViewContentI.CONTENT_ID_KEY");

    static ChangesViewContentI getInstance(Project project) {
        return project.getInstance(ChangesViewContentI.class);
    }

    void addContent(Content content);

    void removeContent(Content content);

    void setSelectedContent(Content content);

    <T> @Nullable T getActiveComponent(Class<T> aClass);

    static @Nullable String getContentId(Content content) {
        return content.getUserData(CONTENT_ID_KEY);
    }

    default void selectContent(String contentId) {
        selectContent(contentId, false);
    }

    void selectContent(String contentId, boolean requestFocus);

    boolean isContentSelected(String contentId);

    @Nullable ToolWindow getToolWindowFor(String contentId);
}
