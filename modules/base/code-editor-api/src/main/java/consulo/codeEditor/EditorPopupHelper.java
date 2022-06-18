/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Mar-22
 */
@Service(ComponentScope.APPLICATION)
public interface EditorPopupHelper {

  /**
   * Allows to get an editor position for which a popup with auxiliary information might be shown.
   * <p/>
   * Primary intention for this key is to hint popup position for the non-caret location.
   */
  public static final Key<VisualPosition> ANCHOR_POPUP_POSITION = Key.create("popup.anchor.position");

  static EditorPopupHelper getInstance() {
    return Application.get().getInstance(EditorPopupHelper.class);
  }

  /**
   * Returns the location where a popup invoked from the specified editor should be displayed.
   *
   * @param editor the editor over which the popup is shown.
   * @return location as close as possible to the action origin.
   */
  @Nonnull
  RelativePoint guessBestPopupLocation(@Nonnull Editor editor);

  /**
   * @param editor the editor over which the popup is shown.
   * @return true if popup location is located in visible area
   * false if center would be suggested instead
   */
  boolean isBestPopupLocationVisible(@Nonnull Editor editor);

  void showPopupInBestPositionFor(@Nonnull Editor editor, @Nonnull JBPopup popup);
}
