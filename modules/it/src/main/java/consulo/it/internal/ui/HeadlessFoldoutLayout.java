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
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.LayoutConstraint;

/**
 * Dummy-but-creatable headless {@link FoldoutLayout}.
 *
 * @author VISTALL
 */
public class HeadlessFoldoutLayout extends HeadlessLayoutBase<LayoutConstraint> implements FoldoutLayout {
    private LocalizeValue myTitle;
    private boolean myState;

    public HeadlessFoldoutLayout(LocalizeValue title, boolean state) {
        myTitle = title;
        myState = state;
    }

    @Override
    public FoldoutLayout setState(boolean showing) {
        myState = showing;
        return this;
    }

    @Override
    public FoldoutLayout setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }
}
