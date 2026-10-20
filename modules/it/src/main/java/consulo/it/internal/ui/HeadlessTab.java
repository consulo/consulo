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
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.internal.PlainTextPresentation;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Headless {@link Tab}. It keeps its renderer, so its title reads as on a rendering frontend.
 *
 * @author VISTALL
 */
public class HeadlessTab implements Tab {
    private final @Nullable HeadlessTabbedLayout myTabbedLayout;

    private BiConsumer<Tab, TextItemPresentation> myRenderer = (tab, presentation) -> presentation.append(tab.toString());

    private boolean myEnabled = true;

    private @Nullable Component mySuffixComponent;

    public HeadlessTab(@Nullable HeadlessTabbedLayout tabbedLayout) {
        myTabbedLayout = tabbedLayout;
    }

    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public LocalizeValue getTitle() {
        return PlainTextPresentation.textOf(presentation -> myRenderer.accept(this, presentation));
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
    }

    @Override
    public void setSuffixComponent(@Nullable Component component) {
        mySuffixComponent = component;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public void update() {
    }

    @Override
    public void select() {
        HeadlessTabbedLayout tabbedLayout = myTabbedLayout;
        if (tabbedLayout != null) {
            tabbedLayout.select(this);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }
}
