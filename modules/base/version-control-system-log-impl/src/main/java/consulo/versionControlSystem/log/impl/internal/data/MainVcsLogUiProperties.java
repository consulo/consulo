/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MainVcsLogUiProperties extends VcsLogUiProperties {

  VcsLogUiProperty<Boolean> SHOW_DETAILS = new VcsLogUiProperty<>("Window.ShowDetails");
  VcsLogUiProperty<Boolean> SHOW_LONG_EDGES = new VcsLogUiProperty<>("Graph.ShowLongEdges");
  VcsLogUiProperty<PermanentGraph.SortType> BEK_SORT_TYPE = new VcsLogUiProperty<>("Graph.BekSortType");
  VcsLogUiProperty<Boolean> SHOW_ROOT_NAMES = new VcsLogUiProperty<>("Table.ShowRootNames");
  VcsLogUiProperty<Boolean> COMPACT_REFERENCES_VIEW = new VcsLogUiProperty<>("Table.CompactReferencesView");
  VcsLogUiProperty<Boolean> SHOW_TAG_NAMES = new VcsLogUiProperty<>("Table.ShowTagNames");
  VcsLogUiProperty<Boolean> TEXT_FILTER_MATCH_CASE = new VcsLogUiProperty<>("TextFilter.MatchCase");
  VcsLogUiProperty<Boolean> TEXT_FILTER_REGEX = new VcsLogUiProperty<>("TextFilter.Regex");

  void addRecentlyFilteredUserGroup(@Nonnull List<String> usersInGroup);

  void addRecentlyFilteredBranchGroup(@Nonnull List<String> valuesInGroup);

  @Nonnull
  List<List<String>> getRecentlyFilteredUserGroups();

  @Nonnull
  List<List<String>> getRecentlyFilteredBranchGroups();

  void saveFilterValues(@Nonnull String filterName, @Nullable List<String> values);

  @Nullable
  List<String> getFilterValues(@Nonnull String filterName);

  @RequiredUIAccess
  void addChangeListener(@Nonnull VcsLogUiPropertiesListener listener);

  @RequiredUIAccess
  void removeChangeListener(@Nonnull VcsLogUiPropertiesListener listener);

  class VcsLogHighlighterProperty extends VcsLogUiProperty<Boolean> {
    private static final Map<String, VcsLogHighlighterProperty> ourProperties = new HashMap<>();
    @Nonnull
    private final String myId;

    public VcsLogHighlighterProperty(@Nonnull String name) {
      super("Highlighter." + name);
      myId = name;
    }

    @Nonnull
    public String getId() {
      return myId;
    }

    @Nonnull
    public static VcsLogHighlighterProperty get(@Nonnull String id) {
      VcsLogHighlighterProperty property = ourProperties.get(id);
      if (property == null) {
        property = new VcsLogHighlighterProperty(id);
        ourProperties.put(id, property);
      }
      return property;
    }
  }

  interface VcsLogUiPropertiesListener {
    <T> void onPropertyChanged(@Nonnull VcsLogUiProperty<T> property);
  }
}
