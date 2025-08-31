/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author spLeaner
 */
public abstract class ConfigurationError implements Comparable<ConfigurationError> {
  private final String myPlainTextTitle;
  private final String myDescription;
  private boolean myIgnored;

  protected ConfigurationError(String plainTextTitle, String description) {
    this(plainTextTitle, description, false);
  }

  protected ConfigurationError(String plainTextTitle, String description, boolean ignored) {
    myPlainTextTitle = plainTextTitle;
    myDescription = description;
    myIgnored = ignored;
  }

  @Nonnull
  public String getPlainTextTitle() {
    return myPlainTextTitle;
  }

  @Nonnull
  public String getDescription() {
    return myDescription;
  }

  /**
   * Called when user invokes "Ignore" action
   * @param "true" if user invokes "Ignore", "false" if user wish to not ignore this error anymore
   */
  public void ignore(boolean b) {
    if (b != myIgnored) {
      myIgnored = b;
    }
  }

  /**
   * @return "true" if this error is ignored
   */
  public boolean isIgnored() {
    return myIgnored;
  }

  /**
   * Called when user invokes "Fix" action
   */
  public void fix(JComponent contextComponent, RelativePoint relativePoint) {
  }

  public boolean canBeFixed() {
    return true;
  }

  public abstract void navigate();

  @Override
  public int compareTo(ConfigurationError o) {
    if (myIgnored != o.isIgnored()) return -1;

    int titleResult = getPlainTextTitle().compareTo(o.getPlainTextTitle());
    if (titleResult != 0) return titleResult;

    int descriptionResult = getDescription().compareTo(o.getDescription());
    if (descriptionResult != 0) return descriptionResult;

    return 0;
  }
}
