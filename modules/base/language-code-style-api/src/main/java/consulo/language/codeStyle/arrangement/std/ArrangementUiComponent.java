/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement.std;

import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.component.extension.ExtensionPointName;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

/**
 * Defines a contract for UI component used at standard arrangement settings managing code.
 * <p/>
 * It's assumed that there is a dedicated implementation of this interface for every {@link StdArrangementTokenUiRole}.
 *
 * @author Denis Zhdanov
 * @since 3/11/13 10:22 AM
 */
public interface ArrangementUiComponent {

  @Nullable
  ArrangementSettingsToken getToken();

  @Nonnull
  Set<ArrangementSettingsToken> getAvailableTokens();

  void chooseToken(@Nonnull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException;

  @Nonnull
  ArrangementMatchCondition getMatchCondition();

  @Nonnull
  JComponent getUiComponent();

  /**
   * We use 'enabled by user' property name here in order to avoid clash
   * with {@link Component#isEnabled() standard awt 'enabled' property}.
   *
   * @return    <code>true</code> if current ui token is enabled; <code>false</code> otherwise
   */
  boolean isEnabled();

  void setEnabled(boolean enabled);

  /**
   * @return screen bounds for the {@link #getUiComponent() target UI component} (if known)
   */
  @Nullable
  Rectangle getScreenBounds();

  boolean isSelected();

  /**
   * Instructs current component that it should {@link #getUiComponent() draw} itself according to the given 'selected' state.
   *
   * @param selected  flag that indicates if current component should be drawn as 'selected'
   */
  void setSelected(boolean selected);

  void setData(@Nonnull Object data);

  void reset();

  /**
   * Notifies current component about mose move event.
   * <p/>
   * Primary intention is to allow to react on event like 'on mouse hover' etc. We can't do that by subscribing to the
   * mouse events at the {@link #getUiComponent() corresponding UI control} because it's used only as a renderer and is not put
   * to the containers hierarchy, hence, doesn't receive mouse events.
   *
   * @param event  target mouse move event
   * @return       bounds to be repainted (in screen coordinates) if any; <code>null</code> otherwise
   */
  @Nullable
  Rectangle onMouseMove(@Nonnull MouseEvent event);

  void onMouseRelease(@Nonnull MouseEvent event);

  @Nullable
  Rectangle onMouseExited();

  @Nullable
  Rectangle onMouseEntered(@Nonnull MouseEvent e);

  /**
   * @param width   the width to get baseline for
   * @param height  the height to get baseline for
   * @return baseline's y coordinate if applicable; negative value otherwise
   */
  int getBaselineToUse(int width, int height);

  void setListener(@Nonnull Listener listener);

  /**
   * Method to process second click on the component,
   * e.g. we can deselect the component or invert it condition
   */
  void handleMouseClickOnSelected();

  /**
   * For condition that can't be disabled,
   * e.g. 'not public' can be used with any other rule like 'private' or 'not private'
   * @return
   */
  boolean alwaysCanBeActive();

  interface Listener {
    void stateChanged();
  }
}
