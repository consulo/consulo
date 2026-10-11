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
package consulo.configurable.internal;

import consulo.ui.Component;
import consulo.ui.Tab;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class ConfigurableSearchHits {
    private final Set<Component> myTargets = new LinkedHashSet<>();
    private final Set<Tab> myTabTargets = new LinkedHashSet<>();
    private final Set<Tab> myTabsToSelect = new LinkedHashSet<>();

    public List<Component> getTargets() {
        return new ArrayList<>(myTargets);
    }

    public List<Tab> getTabTargets() {
        return new ArrayList<>(myTabTargets);
    }

    public List<Tab> getTabsToSelect() {
        return new ArrayList<>(myTabsToSelect);
    }

    public boolean isEmpty() {
        return myTargets.isEmpty() && myTabTargets.isEmpty();
    }

    public void addTarget(Component target) {
        myTargets.add(target);
    }

    void addTabTarget(Tab tab) {
        myTabTargets.add(tab);
    }

    void addTabToSelect(Tab tab) {
        myTabsToSelect.add(tab);
    }

    void addAll(ConfigurableSearchHits hits) {
        myTargets.addAll(hits.myTargets);
        myTabTargets.addAll(hits.myTabTargets);
        myTabsToSelect.addAll(hits.myTabsToSelect);
    }
}
