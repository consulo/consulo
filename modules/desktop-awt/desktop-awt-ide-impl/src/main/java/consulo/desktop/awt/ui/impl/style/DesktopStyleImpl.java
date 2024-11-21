/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl.style;

import consulo.desktop.awt.ui.impl.image.DesktopAWTImage;
import consulo.desktop.awt.ui.plaf.LafWithIconLibrary;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.MorphValue;
import consulo.ui.ex.awt.BuildInLookAndFeel;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.impl.style.StyleImpl;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.ui.style.StyleColorValue;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-08-23
 */
public class DesktopStyleImpl extends StyleImpl {
    private static final Logger LOG = Logger.getInstance(DesktopStyleImpl.class);

    private final MorphValue<Boolean> myDarkValue = MorphValue.of(() -> {
        LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        return lookAndFeel instanceof BuildInLookAndFeel && ((BuildInLookAndFeel) lookAndFeel).isDark();
    });

    private final UIManager.LookAndFeelInfo myLookAndFeelInfo;

    public DesktopStyleImpl(UIManager.LookAndFeelInfo lookAndFeelInfo) {
        myLookAndFeelInfo = lookAndFeelInfo;
    }

    @Nonnull
    @Override
    public ColorValue getColorValue(@Nonnull StyleColorValue colorValue) {
        // maybe time for map?
        if (colorValue == ComponentColors.TEXT || colorValue == ComponentColors.TEXT_FOREGROUND) {
            return TargetAWT.from(UIUtil.getLabelForeground());
        }
        else if (colorValue == ComponentColors.DISABLED_TEXT) {
            return TargetAWT.from(UIUtil.getInactiveTextColor());
        }
        else if (colorValue == ComponentColors.LAYOUT) {
            return TargetAWT.from(UIUtil.getPanelBackground());
        }
        else if (colorValue == ComponentColors.BORDER) {
            return TargetAWT.from(UIUtil.getBorderColor());
        }
        else if (colorValue == StandardColors.BLUE) {
            return TargetAWT.from(JBColor.BLUE);
        }
        else if (colorValue == StandardColors.RED) {
            return TargetAWT.from(JBColor.RED);
        }
        else if (colorValue == StandardColors.GREEN) {
            return TargetAWT.from(JBColor.GREEN);
        }
        else if (colorValue == StandardColors.GRAY) {
            return TargetAWT.from(JBColor.GRAY);
        }
        else if (colorValue == StandardColors.LIGHT_GRAY) {
            return TargetAWT.from(JBColor.LIGHT_GRAY);
        }
        else if (colorValue == StandardColors.DARK_GRAY) {
            return TargetAWT.from(JBColor.DARK_GRAY);
        }
        else if (colorValue == StandardColors.LIGHT_YELLOW) {
            return TargetAWT.from(LightColors.YELLOW);
        }
        else if (colorValue == StandardColors.BLACK) {
            return TargetAWT.from(JBColor.BLACK);
        }
        else if (colorValue == StandardColors.WHITE) {
            return TargetAWT.from(JBColor.WHITE);
        }
        else if (colorValue == StandardColors.MAGENTA) {
            return TargetAWT.from(JBColor.MAGENTA);
        }
        else if (colorValue == StandardColors.YELLOW) {
            return TargetAWT.from(JBColor.YELLOW);
        }
        else if (colorValue == StandardColors.ORANGE) {
            return TargetAWT.from(JBColor.ORANGE);
        }
        else if (colorValue == StandardColors.LIGHT_RED) {
            return TargetAWT.from(LightColors.RED);
        }
        else if (colorValue == StandardColors.CYAN) {
            return TargetAWT.from(JBColor.CYAN);
        }
        LOG.error(new UnsupportedOperationException(colorValue.toString()));
        return TargetAWT.from(JBColor.WHITE);
    }

    @Nonnull
    @Override
    public Image getImage(@Nonnull Image image) {
        if (image instanceof DesktopAWTImage awtImage) {
            return ((DesktopAWTImage) image).copyWithForceLibraryId(getIconLibraryId());
        }
        return image;
    }

    @Nonnull
    public UIManager.LookAndFeelInfo getLookAndFeelInfo() {
        return myLookAndFeelInfo;
    }

    @Nonnull
    public String getClassName() {
        return myLookAndFeelInfo.getClassName();
    }

    @Nonnull
    @Override
    public String getName() {
        return myLookAndFeelInfo.getName();
    }

    @Nonnull
    @Override
    public String getIconLibraryId() {
        if (myLookAndFeelInfo instanceof LafWithIconLibrary) {
            return ((LafWithIconLibrary) myLookAndFeelInfo).getIconLibraryId();
        }
        return IconLibraryManager.LIGHT_LIBRARY_ID;
    }

    @Override
    public boolean isDark() {
        return myDarkValue.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DesktopStyleImpl that = (DesktopStyleImpl) o;
        return Objects.equals(myLookAndFeelInfo, that.myLookAndFeelInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myLookAndFeelInfo);
    }

    @Override
    public String toString() {
        return myLookAndFeelInfo.toString();
    }
}
