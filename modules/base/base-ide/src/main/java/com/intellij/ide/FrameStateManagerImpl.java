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
package com.intellij.ide;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.BusyObject;
import consulo.disposer.Disposer;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Singleton
public class FrameStateManagerImpl extends FrameStateManager {
  private final List<FrameStateListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private boolean myShouldSynchronize;
  private final Alarm mySyncAlarm;

  private final BusyObject.Impl myActive;
  private final ApplicationEx myApp;

  @Inject
  public FrameStateManagerImpl(final ApplicationEx app) {
    myApp = app;
    myActive = new BusyObject.Impl() {
      @Override
      public boolean isReady() {
        return myApp.isActive();
      }
    };

    myShouldSynchronize = false;
    mySyncAlarm = new Alarm();

    app.getMessageBus().connect().subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener.Adapter() {
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
    for (FrameStateListener listener : myListeners) {
      listener.onFrameDeactivated();
    }
  }

  private void fireActivationEvent() {
    for (FrameStateListener listener : myListeners) {
      listener.onFrameActivated();
    }
  }

  @Override
  public void addListener(@Nonnull FrameStateListener listener) {
    addListener(listener, null);
  }

  @Override
  public void addListener(@Nonnull final FrameStateListener listener, @Nullable Disposable disposable) {
    myListeners.add(listener);
    if (disposable != null) {
      Disposer.register(disposable, () -> removeListener(listener));
    }
  }

  @Override
  public void removeListener(@Nonnull FrameStateListener listener) {
    myListeners.remove(listener);
  }
}
