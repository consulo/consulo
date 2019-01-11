/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.impl.stores.ComponentStoreImpl;
import com.intellij.openapi.diagnostic.Logger;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-12-26
 */
public class IdeAWTUIAccessImpl extends AWTUIAccessImpl {
  public static IdeAWTUIAccessImpl ourInstance = new IdeAWTUIAccessImpl();

  private static Logger LOG = Logger.getInstance(IdeAWTUIAccessImpl.class);

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    ComponentStoreImpl.assertIfInsideSavingSession();

    if (Application.get().isWriteAccessAllowed()) {
      //LOG.warn(new IllegalStateException("Invoking #giveAndWait() from write-thread"));
    }
    super.giveAndWait(runnable);
  }
}
