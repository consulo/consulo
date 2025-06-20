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

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * User: spLeaner
 */
public interface StatusBarWidget extends Disposable {
    @Nonnull
    @Deprecated(forRemoval = true)
    default String ID() {
        return getId();
    }

    @Nonnull
    String getId();

    @Nullable
    WidgetPresentation getPresentation();

    void install(@Nonnull final StatusBar statusBar);

    @RequiredUIAccess
    default void beforeUpdate() {
    }

    interface Multiframe extends StatusBarWidget {
        StatusBarWidget copy();
    }

    interface WidgetPresentation {
        @Nullable
        String getTooltipText();

        @Nullable
        default String getShortcutText() {
            return null;
        }

        @Nullable
        Consumer<MouseEvent> getClickConsumer();
    }

    interface IconPresentation extends WidgetPresentation {
        @Nullable
        Image getIcon();
    }

    interface TextPresentation extends WidgetPresentation {
        @Nonnull
        String getText();

        float getAlignment();
    }

    interface MultipleTextValuesPresentation extends WidgetPresentation {
        /**
         * @return null means the widget is unable to show the popup
         */
        @Nullable
        ListPopup getPopupStep();

        @Nullable
        @RequiredUIAccess
        String getSelectedValue();

        @Nonnull
        @Deprecated
        default String getMaxValue() {
            return "";
        }

        @Nullable
        default Image getIcon() {
            return null;
        }
    }
}
