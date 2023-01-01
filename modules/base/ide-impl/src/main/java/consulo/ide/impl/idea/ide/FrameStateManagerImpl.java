/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.event.FrameStateListener;
import consulo.component.util.BusyObject;
import consulo.disposer.Disposable;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
@ServiceImpl
public class FrameStateManagerImpl extends FrameStateManager {
  private final EventDispatcher<FrameStateListener> myDispatcher = EventDispatcher.create(FrameStateListener.class);

  private boolean myShouldSynchronize;
  private final Alarm mySyncAlarm;

  private final BusyObject.Impl myActive;
  private final Application myApp;

  @Inject
  public FrameStateManagerImpl(final Application app) {
    myApp = app;
    myActive = new BusyObject.Impl() {
      @Override
      public boolean isReady() {
        return myApp.isActive();
      }
    };

    myShouldSynchronize = false;
    mySyncAlarm = new Alarm();

    app.getMessageBus().connect().subscribe(ApplicationActivationListener.class, new ApplicationActivationListener.Adapter() {
      @Override
      public void applicationActivated(IdeFrame ideFrame) {
        myActive.onReady();
        mySyncAlarm.cancelAllRequests();
        if (myShouldSynchronize) {
          myShouldSynchronize = false;
          fireActivationEvent();
        }
      }

      @Override
      public void applicationDeactivated(IdeFrame ideFrame) {
        mySyncAlarm.cancelAllRequests();
        mySyncAlarm.addRequest(new Runnable() {
          @Override
          public void run() {
            if (!app.isActive()) {
              myShouldSynchronize = true;
              fireDeactivationEvent();
            }
          }
        }, 200);
      }
    });
  }

  @Override
  public AsyncResult<Void> getApplicationActive() {
    return myActive.getReady(this);
  }

  private void fireDeactivationEvent() {
    myDispatcher.getMulticaster().onFrameDeactivated();
  }

  private void fireActivationEvent() {
    myDispatcher.getMulticaster().onFrameActivated();
  }

  @Override
  public void addListener(@Nonnull FrameStateListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void addListener(@Nonnull final FrameStateListener listener, @Nonnull Disposable disposable) {
    myDispatcher.addListener(listener, disposable);
  }

  @Override
  public void removeListener(@Nonnull FrameStateListener listener) {
    myDispatcher.addListener(listener);
  }
}
