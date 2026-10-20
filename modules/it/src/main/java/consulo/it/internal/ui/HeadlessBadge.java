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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public class HeadlessBadge extends HeadlessComponentBase implements Badge {
    private final Set<BadgeStyle> myStyles = EnumSet.noneOf(BadgeStyle.class);
    private LocalizeValue myText;
    private @Nullable Image myIcon;

    public HeadlessBadge(LocalizeValue text) {
        myText = text;
    }

    @Override
    public void addStyle(BadgeStyle style) {
        myStyles.add(style);
    }

    public Set<BadgeStyle> getStyles() {
        return myStyles;
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public void setText(LocalizeValue text) {
        myText = text;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }
}
