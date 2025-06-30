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
package consulo.configurable;

import consulo.util.collection.ArrayFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Named component which provides a configuration user interface.
 *
 * @see SearchableConfigurable
 * @see SimpleConfigurable
 *
 * @see ApplicationConfigurable
 * @see ProjectConfigurable
 */
public interface Configurable extends UnnamedConfigurable {
  Configurable[] EMPTY_ARRAY = new Configurable[0];

  ArrayFactory<Configurable> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new Configurable[count];

  @Nonnull
  default String getId() {
    throw new AbstractMethodError("#getId() implementation required for class " + getClass());
  }

  @Nullable
  default String getParentId() {
    return null;
  }

  /**
   * can be used inside {@link #getHelpTopic()} for disable help
   */
  String DISABLED_HELP_ID = "___disabled___";

  /**
   * Returns the user-visible name of the settings component.
   *
   * @return the visible name of the component.
   */
  default String getDisplayName() {
    return null;
  }

  /**
   * Returns alternative id for help (not id of configurable)
   *
   * @return the help id
   */
  @Override
  @Nullable
  default String getHelpTopic() {
    return null;
  }

  interface Composite {
    @Nonnull
    Configurable[] getConfigurables();
  }

  /**
   * Forbids wrapping the content of the configurable in a scroll pane. Required when
   * the configurable contains its own scrollable components.
   */
  interface NoScroll {
  }

  /**
   * This marker interface notifies the Settings dialog to not add an empty border to the Swing form.
   * Required when the Swing form is a tabbed pane.
   */
  interface NoMargin {
  }

  @Deprecated
  interface HoldPreferredFocusedComponent extends UnnamedConfigurable {
  }
}
