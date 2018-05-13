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
package consulo.vfs.newvfs;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl;
import com.intellij.openapi.vfs.newvfs.RefreshSessionImpl;
import consulo.application.TransactionGuardEx;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-05-13
 */
public class NewRefreshQueueImpl extends RefreshQueueImpl {
  private static final Logger LOG = Logger.getInstance(NewRefreshQueueImpl.class);

  @Override
  public void execute(@Nonnull RefreshSessionImpl session) {
    if (session.isAsynchronous()) {
      queueSession(session, session.getTransaction());
    }
    else {
      Application app = ApplicationManager.getApplication();
      if (app.isWriteThread()) {
        doScan(session);
        session.fireEvents();
      }
      else {
        if (((ApplicationEx)app).holdsReadLock()) {
          LOG.error("Do not call synchronous refresh under read lock (except from WT) - this will cause a deadlock if there are any events to fire.");
          return;
        }
        queueSession(session, TransactionGuard.getInstance().getContextTransaction());
        session.waitFor();
      }
    }
  }

  @Nonnull
  @Override
  protected AccessToken createHeavyLatch(String id) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }
}
