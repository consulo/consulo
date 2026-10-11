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
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;

/**
 * Headless {@link TwoComponentSplitLayout} keeping its two sides.
 *
 * @author VISTALL
 */
public class HeadlessTwoComponentSplitLayout extends HeadlessSplitSlots implements TwoComponentSplitLayout {
    private static final int FIRST = 0;
    private static final int SECOND = 1;

    public HeadlessTwoComponentSplitLayout() {
        super(2);
    }

    @Override
    public void setProportion(int percent) {
    }

    @Override
    public void setPosition(SplitLayoutPosition position) {
    }

    @Override
    public void setFirstComponent(Component component) {
        setSlot(FIRST, component);
    }

    @Override
    public void setSecondComponent(Component component) {
        setSlot(SECOND, component);
    }
}
