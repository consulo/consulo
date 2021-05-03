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
package consulo.ui.web.servlet;

import consulo.ui.Label;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.web.internal.WebUIAccessImpl;
import consulo.web.application.WebSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public class VaadinWebSessionImpl implements WebSession {
  private final UIAccess myAccess;

  @RequiredUIAccess
  public VaadinWebSessionImpl() {
    myAccess = UIAccess.get();
  }

  @Nullable
  @Override
  public UIAccess getAccess() {
    return myAccess.isValid() ? myAccess : null;
  }

  @Override
  public void close() {
    myAccess.give(() -> {
      Window window = Window.create("Consulo", WindowOptions.builder().disableResize().disableClose().build());
      window.setContent(Label.create("Session Closed"));

      window.show();

      ((WebUIAccessImpl)myAccess).getUI().close();
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WebSession copy() {
    // copy state
    return new VaadinWebSessionImpl();
  }
}
