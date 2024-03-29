/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt.speedSearch;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.util.dataholder.Key;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author spLeaner
 * @author Konstantin Bulenkov
 */
public abstract class SpeedSearchSupply {
  /**
   * Client property key to use in jcomponents for passing the actual search query to renderers
   */
  public static final String SEARCH_QUERY_KEY = "SEARCH_QUERY";
  private static final Key SPEED_SEARCH_COMPONENT_MARKER = new Key("SPEED_SEARCH_COMPONENT_MARKER");
  public static final Key<String> SPEED_SEARCH_CURRENT_QUERY = Key.create("SPEED_SEARCH_CURRENT_QUERY");
  public static final String ENTERED_PREFIX_PROPERTY_NAME = "enteredPrefix";

  @Nullable
  public static SpeedSearchSupply getSupply(@Nonnull final JComponent component) {
    return getSupply(component, false);
  }

  @Nullable
  public static SpeedSearchSupply getSupply(@Nonnull final JComponent component, boolean evenIfInactive) {
    SpeedSearchSupply speedSearch = (SpeedSearchSupply)component.getClientProperty(SPEED_SEARCH_COMPONENT_MARKER);

    if (evenIfInactive) {
      return speedSearch;
    }

    return speedSearch != null && speedSearch.isPopupActive() ? speedSearch : null;
  }

  @Nullable
  public abstract Iterable<MatcherTextRange> matchingFragments(@Nonnull final String text);

  /**
   * Selects element according to search criteria changes
   */
  public abstract void refreshSelection();

  public abstract boolean isPopupActive();

  @Nullable
  public String getEnteredPrefix() {
    return null;
  }

  protected void installSupplyTo(final JComponent component) {
    component.putClientProperty(SPEED_SEARCH_COMPONENT_MARKER, this);
    addChangeListener(evt -> component.repaint());
  }

  public abstract void addChangeListener(@Nonnull PropertyChangeListener listener);

  public abstract void removeChangeListener(@Nonnull PropertyChangeListener listener);

  /**
   * Find an element matching the searching query in the underlying component and select it there. Speed-search popup is not affected.
   *
   * @param searchQuery text that the selected element should match
   */
  public abstract void findAndSelectElement(@Nonnull String searchQuery);

  public boolean isObjectFilteredOut(Object o) {
    return false;
  }
}
