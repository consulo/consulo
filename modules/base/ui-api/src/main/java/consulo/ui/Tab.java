/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public interface Tab {
    void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer);

    /**
     * The caption the renderer gives this tab, as a value rather than as drawn: the runs it appends, in order, joined
     * into one. A caption of a single run is that run itself, so a localized caption keeps its key. Icons, suffixes
     * and tooltips are not part of it.
     * <p>
     * The renderer is run against a presentation which only records what it is told, so asking draws nothing, and a
     * tab whose layout was never shown answers the same as one on screen.
     */
    LocalizeValue getTitle();

    void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler);

    /**
     * The group shown on a right click of the tab itself. Whoever builds the tab knows what it stands for, so
     * the group is given rather than looked up from the data below it - the content of a tab which was never
     * selected is not in the ui yet, and nothing can be read off it.
     */
    default void setPopupGroup(String groupId, String place) {
    }

    void update();

    void select();

    @RequiredUIAccess
    default void setEnabled(boolean enabled) {
    }

    default boolean isEnabled() {
        return true;
    }
}
