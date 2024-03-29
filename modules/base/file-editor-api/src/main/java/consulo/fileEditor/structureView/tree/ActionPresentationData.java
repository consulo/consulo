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
package consulo.fileEditor.structureView.tree;

import consulo.ui.image.Image;

import jakarta.annotation.Nullable;

/**
 * The default implementation of the ActionPresentation interface, specifying the presentation
 * information for a grouping, sorting or filtering action displayed in a generic tree.
 */
public class ActionPresentationData implements ActionPresentation {
  private final String myText;
  private final String myDescription;
  private final Image myIcon;

  /**
   * Creates an action presentation with the specified text, description and icon.
   * @param text        the name of the action, displayed in the tooltip for the toolbar button.
   * @param description the description of the action, displayed in the status bar when the mouse
   *                    is over the toolbar button.
   * @param icon        the icon for the action, displayed on the toolbar button.
   */

  public ActionPresentationData(String text, String description, @Nullable Image icon) {
    myText = text;
    myDescription = description;
    myIcon = icon;
  }

  @Override
  public String getText() {
    return myText;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }
}
