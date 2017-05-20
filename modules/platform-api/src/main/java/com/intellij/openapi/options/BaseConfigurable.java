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
package com.intellij.openapi.options;

import javax.swing.*;


public abstract class BaseConfigurable implements Configurable, Configurable.HoldPreferredFocusedComponent {
  protected boolean myModified;

  @Override
  public boolean isModified() {
    return myModified;
  }

  protected void setModified(final boolean modified) {
    myModified = modified;
  }

  /**
   * @return component which should be focused when the dialog appears
   *         on the screen.
   */
  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }
}