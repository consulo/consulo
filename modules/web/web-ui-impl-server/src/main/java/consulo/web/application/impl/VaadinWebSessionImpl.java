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
package consulo.web.application.impl;

import consulo.ui.*;
import consulo.web.application.WebSession;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public class VaadinWebSessionImpl implements WebSession {
  private final VaadinUIAccessImpl myVaadinUI;

  @RequiredUIAccess
  public VaadinWebSessionImpl() {
    myVaadinUI = (VaadinUIAccessImpl)UIAccess.get();
  }

  @Override
  public void close() {
    myVaadinUI.give(() -> {
      Window window = Windows.modalWindow("Consulo");
      window.setContent(Components.label("Session Closed"));
      window.setResizable(false);
      window.setClosable(false);

      window.show();

      myVaadinUI.getUI().close();
    });
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public WebSession copy() {
    // copy state
    return new VaadinWebSessionImpl();
  }
}
