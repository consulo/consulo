/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.util;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class DiffGutterRenderer extends GutterIconRenderer {
    @Nonnull
    private final Image myIcon;
    @Nonnull
    private final LocalizeValue myTooltip;

    public DiffGutterRenderer(@Nonnull Image icon, @Nonnull LocalizeValue tooltip) {
        myIcon = icon;
        myTooltip = tooltip;
    }

    public DiffGutterRenderer(@Nonnull Image icon, @Nullable String tooltip) {
        this(icon, LocalizeValue.ofNullable(tooltip));
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Nonnull
    @Override
    public LocalizeValue getTooltipValue() {
        return myTooltip;
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Nonnull
    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
        return new DumbAwareAction() {
            @Override
            public void actionPerformed(AnActionEvent e) {
                performAction(e);
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    protected abstract void performAction(AnActionEvent e);
}
