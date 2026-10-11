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
package consulo.ui.impl;

import consulo.ui.Component;
import consulo.ui.Spotlight;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public abstract class BaseSpotlight implements Spotlight {
    private final Layout<?> myLayout;

    private List<Component> myTargets = List.of();

    private List<Tab> myTabTargets = List.of();

    private boolean myDisposed;

    protected BaseSpotlight(Layout<?> layout) {
        myLayout = layout;
    }

    @Override
    public Layout<?> getLayout() {
        return myLayout;
    }

    @Override
    public List<Component> getTargets() {
        return myTargets;
    }

    @Override
    public List<Tab> getTabTargets() {
        return myTabTargets;
    }

    public boolean isLit() {
        return !myTargets.isEmpty() || !myTabTargets.isEmpty();
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    @RequiredUIAccess
    @Override
    public final void setTargets(Collection<? extends Component> targets, Collection<? extends Tab> tabTargets) {
        if (myDisposed) {
            return;
        }

        List<Component> newTargets = List.copyOf(new LinkedHashSet<>(targets));
        List<Tab> newTabTargets = List.copyOf(new LinkedHashSet<>(tabTargets));
        if (newTargets.equals(myTargets) && newTabTargets.equals(myTabTargets)) {
            return;
        }

        List<Component> oldTargets = myTargets;
        myTargets = newTargets;
        myTabTargets = newTabTargets;

        onTargetsChanged(oldTargets, newTargets);
    }

    @Override
    public final void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        List<Component> oldTargets = myTargets;
        myTargets = List.of();
        myTabTargets = List.of();

        onDispose(oldTargets);
    }

    @RequiredUIAccess
    protected abstract void onTargetsChanged(List<Component> oldTargets, List<Component> newTargets);

    protected abstract void onDispose(List<Component> oldTargets);
}
