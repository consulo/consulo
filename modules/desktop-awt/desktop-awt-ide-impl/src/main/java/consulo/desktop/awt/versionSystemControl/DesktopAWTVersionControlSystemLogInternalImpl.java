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
package consulo.desktop.awt.versionSystemControl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.popup.Balloon;
import consulo.versionControlSystem.log.internal.VersionControlSystemLogInternal;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceImpl
@Singleton
public class DesktopAWTVersionControlSystemLogInternalImpl implements VersionControlSystemLogInternal {
    @Override
    public void showToolTip(@Nonnull JTable table, @Nonnull String text, @Nonnull MouseEvent e) {
        // standard tooltip does not allow to customize its location, and locating tooltip above can obscure some important info
        Point point = new Point(e.getX() + 5, e.getY());

        JEditorPane tipComponent = IdeTooltipManagerImpl.initPane(text, new HintHint(table, point).setAwtTooltip(true), null);
        IdeTooltip tooltip = new IdeTooltip(table, point, new Wrapper(tipComponent)).setPreferredPosition(Balloon.Position.atRight);
        IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, false);
    }

    @Override
    public void showToolTip(@Nonnull JTable myTable, Point point, JComponent tipComponent, boolean now) {
        IdeTooltip tooltip =
            new IdeTooltip(myTable, point, new Wrapper(tipComponent)).setPreferredPosition(Balloon.Position.below);
        IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, now);
    }

    @Override
    public void hideToolTip(@Nonnull MouseEvent e) {
        if (IdeTooltipManagerImpl.getInstanceImpl().hasCurrent()) {
            IdeTooltipManagerImpl.getInstanceImpl().hideCurrent(e);
        }
    }
}
