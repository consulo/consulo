/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.action;

import consulo.ui.ex.internal.HelpTooltip;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.border.Border;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 22-Jul-22
 */
@Deprecated
public interface ActionButton extends AnActionHolder, ActionButtonComponent {
    /**
     * By default button representing popup action group displays 'dropdown' icon.
     * This key allows to avoid 'dropdown' icon painting, just put it in ActionButtonImpl's presentation or template presentation of ActionGroup like this:
     * <code>presentation.putClientProperty(ActionButtonImpl.HIDE_DROPDOWN_ICON, Boolean.TRUE)</code>
     */

    public static final Key<Boolean> HIDE_DROPDOWN_ICON = Key.create("HIDE_DROPDOWN_ICON");

    void setVisible(boolean visible);

    boolean isVisible();

    @Nonnull
    Presentation getPresentation();

    Rectangle getBounds();

    void setIconOverrider(@Nullable Function<ActionButton, Image> imageCalculator);

    void updateToolTipText();

    void click();

    void setNoIconsInPopup(boolean value);

    void setBorder(Border border);

    void setOpaque(boolean value);

    default void setCustomTooltipBuilder(BiConsumer<HelpTooltip, Presentation> builder) {
    }

    void setCustomShortcutBuilder(Supplier<String> shortcutBuilder);
}
