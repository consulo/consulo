/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
public class WebToolWindowContentUI implements ContentUI {
  @Override
  public void setManager(@NotNull ContentManager manager) {

  }

  @Override
  public boolean isSingleSelection() {
    return false;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return false;
  }

  @Override
  public boolean canBeEmptySelection() {
    return false;
  }

  @Override
  public void beforeDispose() {

  }

  @Override
  public boolean canChangeSelectionTo(@NotNull Content content, boolean implicit) {
    return false;
  }

  @NotNull
  @Override
  public String getCloseActionName() {
    return null;
  }

  @NotNull
  @Override
  public String getCloseAllButThisActionName() {
    return null;
  }

  @NotNull
  @Override
  public String getPreviousContentActionName() {
    return null;
  }

  @NotNull
  @Override
  public String getNextContentActionName() {
    return null;
  }

  @Override
  public JComponent getComponent() {
    return null;
  }

  @Override
  public void dispose() {

  }
}
