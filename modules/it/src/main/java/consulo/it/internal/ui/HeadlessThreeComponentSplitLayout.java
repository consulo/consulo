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

import consulo.ui.Component;
import consulo.ui.layout.ThreeComponentSplitLayout;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link ThreeComponentSplitLayout} keeping its three panes.
 *
 * @author VISTALL
 */
public class HeadlessThreeComponentSplitLayout extends HeadlessSplitSlots implements ThreeComponentSplitLayout {
    private static final int FIRST = 0;
    private static final int CENTER = 1;
    private static final int SECOND = 2;

    public HeadlessThreeComponentSplitLayout() {
        super(3);
    }

    @Override
    public ThreeComponentSplitLayout setFirstComponent(@Nullable Component component) {
        setSlot(FIRST, component);
        return this;
    }

    @Override
    public ThreeComponentSplitLayout setCenterComponent(@Nullable Component component) {
        setSlot(CENTER, component);
        return this;
    }

    @Override
    public ThreeComponentSplitLayout setSecondComponent(@Nullable Component component) {
        setSlot(SECOND, component);
        return this;
    }
}
