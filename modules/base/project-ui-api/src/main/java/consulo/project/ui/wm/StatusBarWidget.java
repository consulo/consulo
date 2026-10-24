/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * A widget of the status bar. A widget gives a {@link WidgetPresentation}, and the status bar shows it as a toolbar-style
 * button: text and icon, tooltip, click, popup and hover look the same as every other widget's. Only content which a
 * button cannot show is a {@link CustomStatusBarWidget}.
 *
 * @author spLeaner
 */
public interface StatusBarWidget extends Disposable {
    Key<StatusBarWidget> KEY = Key.create(StatusBarWidget.class);

    @Deprecated(forRemoval = true)
    default String ID() {
        return getId();
    }

    String getId();

    @Nullable WidgetPresentation getPresentation();

    void install(StatusBar statusBar);

    @RequiredUIAccess
    default void beforeUpdate() {
    }

    interface Multiframe extends StatusBarWidget {
        StatusBarWidget copy();
    }

    interface WidgetPresentation {
        LocalizeValue getTooltipText();

        default @Nullable String getShortcutText() {
            return null;
        }

        default @Nullable ComponentEventListener<Component, ClickEvent> getClickListener() {
            return null;
        }

        default boolean isEnabled() {
            return true;
        }
    }

    interface IconPresentation extends WidgetPresentation {
        @Nullable Image getIcon();
    }

    interface TextPresentation extends WidgetPresentation {
        String getText();
    }

    interface MultipleTextValuesPresentation extends WidgetPresentation {
        /**
         * @param dataContext the context of the widget the popup is opened from
         * @return null means the widget is unable to show the popup
         */
        @RequiredUIAccess
        @Nullable ListPopup getPopupStep(DataContext dataContext);

        @RequiredUIAccess
        @Nullable String getSelectedValue();

        default @Nullable Image getIcon() {
            return null;
        }

        /**
         * An icon drawn after the selected value - a state of the value, such as commits waiting to be pulled or pushed.
         */
        default @Nullable Image getSecondaryIcon() {
            return null;
        }
    }
}
