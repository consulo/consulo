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
package consulo.ide.impl.codeEditor;

import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10-Apr-22
 */
@Singleton
public class EditorPopupHelperImpl implements EditorPopupHelper {
  private PopupFactoryImpl myPopupFactory;

  @Inject
  public EditorPopupHelperImpl(JBPopupFactory factory) {
    myPopupFactory = (PopupFactoryImpl)factory;
  }

  @Nonnull
  @Override
  public RelativePoint guessBestPopupLocation(@Nonnull Editor editor) {
    return myPopupFactory.guessBestPopupLocation(editor);
  }

  @Override
  public boolean isBestPopupLocationVisible(@Nonnull Editor editor) {
    return myPopupFactory.isBestPopupLocationVisible(editor);
  }

  @Override
  public void showPopupInBestPositionFor(@Nonnull Editor editor, @Nonnull JBPopup popup) {
    ((AbstractPopup)popup).showInBestPositionFor(editor);
  }
}
