/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableHit;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class OptionsEditorFilter {
    private final OptionsEditorContext myContext;
    private final Project myProject;
    private final Supplier<Configurable[]> myConfigurables;
    private final SearchableOptionsRegistrar myIndex = SearchableOptionsRegistrar.getInstance();
    private final Map<Configurable, String> myConfigurableToLastOption = new HashMap<>();

    private @Nullable Set<Configurable> myFiltered;
    private @Nullable ConfigurableHit myHits;
    private @Nullable Configurable myLastSelected;
    private @Nullable OptionsLiveIndex myLiveIndex;

    public OptionsEditorFilter(OptionsEditorContext context, Project project, Supplier<Configurable[]> configurables) {
        myContext = context;
        myProject = project;
        myConfigurables = configurables;
    }

    public void setLiveIndex(@Nullable OptionsLiveIndex liveIndex) {
        myLiveIndex = liveIndex;
    }

    public boolean shouldBeShowing(Configurable configurable) {
        return myFiltered == null || myFiltered.contains(configurable) || isChildOfNameHit(configurable);
    }

    private boolean isChildOfNameHit(Configurable configurable) {
        ConfigurableHit hits = myHits;
        if (hits == null) {
            return false;
        }

        Configurable each = configurable;
        while (each != null) {
            if (hits.getNameFullHits().contains(each)) {
                return true;
            }
            each = myContext.getParentConfigurable(each);
        }
        return false;
    }

    public @Nullable Configurable update(String text, OptionsEditorFilterChange change, boolean adjustSelection) {
        if (text.trim().isEmpty()) {
            myContext.setHoldingFilter(false);
            myFiltered = null;
        }
        else {
            myContext.setHoldingFilter(true);
            ConfigurableHit hits = myIndex.getConfigurables(
                myConfigurables.get(),
                change == OptionsEditorFilterChange.CHANGE,
                myFiltered,
                text,
                myProject
            );

            OptionsLiveIndex liveIndex = myLiveIndex;
            if (liveIndex != null) {
                hits.getContentHits().addAll(liveIndex.findHits(text));
            }

            myHits = hits;
            myFiltered = hits.getAll();
        }

        Set<Configurable> filtered = myFiltered;
        ConfigurableHit hits = myHits;
        Configurable current = myContext.getCurrentConfigurable();

        boolean shouldMoveSelection = true;

        if (hits != null && (hits.getNameFullHits().contains(current) || hits.getContentHits().contains(current))) {
            shouldMoveSelection = false;
        }

        if (shouldMoveSelection && change != OptionsEditorFilterChange.INSERT && (filtered == null || filtered.contains(current))) {
            shouldMoveSelection = false;
        }

        Configurable toSelect = adjustSelection ? current : null;
        if (shouldMoveSelection && hits != null) {
            if (!hits.getNameHits().isEmpty()) {
                toSelect = suggestToSelect(hits.getNameHits(), hits.getNameFullHits());
            }
            else if (!hits.getContentHits().isEmpty()) {
                toSelect = suggestToSelect(hits.getContentHits(), null);
            }
        }

        if ((filtered == null || !filtered.isEmpty()) && toSelect == null && myLastSelected != null) {
            toSelect = myLastSelected;
            myLastSelected = null;
        }

        if (toSelect == null && current != null) {
            myLastSelected = current;
        }

        return toSelect;
    }

    public boolean isNothingFound() {
        Set<Configurable> filtered = myFiltered;
        return filtered != null && filtered.isEmpty();
    }

    public @Nullable ConfigurableHit getHits() {
        return myHits;
    }

    public void enableSearch(Configurable configurable, String text) {
        try {
            boolean sameText = myConfigurableToLastOption.containsKey(configurable)
                && text.equals(myConfigurableToLastOption.get(configurable));

            boolean pushFilteringFurther = true;
            ConfigurableHit hits = myHits;
            if (sameText) {
                pushFilteringFurther = false;
            }
            else if (hits != null) {
                pushFilteringFurther = !hits.getNameHits().contains(configurable);
            }

            Runnable ownSearch = configurable instanceof SearchableConfigurable searchable ? searchable.enableSearch(text) : null;
            if (pushFilteringFurther && ownSearch != null) {
                ownSearch.run();
            }
        }
        finally {
            myConfigurableToLastOption.put(configurable, text);
        }
    }

    private static boolean isEmptyParent(Configurable configurable) {
        return configurable instanceof SearchableConfigurable.Parent parent && !parent.hasOwnContent();
    }

    private static @Nullable Configurable suggestToSelect(Set<Configurable> set, @Nullable Set<Configurable> fullHits) {
        Configurable candidate = null;
        for (Configurable each : set) {
            if (fullHits != null && fullHits.contains(each)) {
                return each;
            }
            if (!isEmptyParent(each) && candidate == null) {
                candidate = each;
            }
        }

        return candidate;
    }
}
