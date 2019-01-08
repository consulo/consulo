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

import com.intellij.openapi.components.impl.stores.ComponentStoreImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.AsyncResult;
import com.vaadin.ui.UI;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class VaadinUIAccessImpl implements UIAccess {
  private static final Logger LOGGER = Logger.getInstance(VaadinUIAccessImpl.class);

  private final UI myUI;

  public VaadinUIAccessImpl(UI ui) {
    myUI = ui;
  }

  @Override
  public boolean isValid() {
    return myUI.isAttached() && myUI.getSession() != null;
  }

  @Override
  public <T> AsyncResult<T> give(@Nonnull Supplier<T> supplier) {
    AsyncResult<T> result = new AsyncResult<>();
    if (isValid()) {
      myUI.access(() -> {
        try {
          result.setDone(supplier.get());
        }
        catch (Throwable e) {
          LOGGER.error(e);
          result.rejectWithThrowable(e);
        }
      });
    }
    else {
      result.setDone();
    }
    return result;
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    ComponentStoreImpl.assertIfInsideSavingSession();

    if (isValid()) {
      myUI.accessSynchronously(runnable);
    }
  }

  public UI getUI() {
    return myUI;
  }
}
