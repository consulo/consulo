/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import com.vaadin.ui.UI;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class VaadinUIAccessImpl implements UIAccess {
  private final UI myUI;

  public VaadinUIAccessImpl(UI ui) {
    myUI = ui;
  }

  @Override
  public boolean isValid() {
    return myUI.isAttached() && myUI.getSession() != null;
  }

  @Override
  public void give(@RequiredUIAccess @Nonnull Runnable runnable) {
    if (isValid()) {
      myUI.access(runnable);
    }
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    if (isValid()) {
      myUI.accessSynchronously(runnable);
    }
  }

  public UI getUI() {
    return myUI;
  }
}
