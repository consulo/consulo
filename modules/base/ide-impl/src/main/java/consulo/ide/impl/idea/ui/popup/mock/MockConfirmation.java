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
package consulo.ide.impl.idea.ui.popup.mock;

import consulo.ui.ex.popup.ListPopupStep;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author Sergey.Vasiliev
 * @since 2004-11-21
 */
public class MockConfirmation extends ListPopupImpl {
  String myOnYesText;
  public MockConfirmation(ListPopupStep aStep, String onYesText) {
    super(aStep);
    myOnYesText = onYesText;
  }

  public void showInCenterOf(@Nonnull Component aContainer) {
    getStep().onChosen(myOnYesText, true);
  }

  public void showUnderneathOf(@Nonnull Component aComponent) {
    getStep().onChosen(myOnYesText, true);
  }
}
