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
import consulo.ui.Tab;
import consulo.ui.impl.BaseSpotlight;
import consulo.ui.layout.Layout;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class HeadlessSpotlight extends BaseSpotlight {
    private List<Component> myLitTargets = List.of();

    private List<Tab> myLitTabs = List.of();

    private int myUpdateCount;

    private boolean myAttached;

    public HeadlessSpotlight(Layout<?> layout) {
        super(layout);

        myAttached = !isDisposed();
    }

    public List<Component> getLitTargets() {
        return myLitTargets;
    }

    public List<Tab> getLitTabs() {
        return myLitTabs;
    }

    public int getUpdateCount() {
        return myUpdateCount;
    }

    public boolean isAttached() {
        return myAttached;
    }

    @Override
    protected void onTargetsChanged(List<Component> oldTargets, List<Component> newTargets) {
        myLitTargets = newTargets;
        myLitTabs = getTabTargets();
        myUpdateCount++;
    }

    @Override
    protected void onDispose(List<Component> oldTargets) {
        myLitTargets = List.of();
        myLitTabs = List.of();
        myAttached = false;
    }
}
