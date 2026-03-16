/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.desktop.awt.ui.keymap.IdeMouseEventDispatcher;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.popup.IdePopupEventDispatcher;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
@ServiceImpl
@Singleton
public class IdeEventQueueProxyImpl implements IdeEventQueueProxy {
  private final IdeEventQueue myIdeEventQueue;

  public IdeEventQueueProxyImpl() {
    myIdeEventQueue = IdeEventQueue.getInstance();
  }

  @Override
  public void flushDelayedKeyEvents() {
    myIdeEventQueue.flushDelayedKeyEvents();
  }

  @Override
  public boolean closeAllPopups(boolean forceRestoreFocus) {
    return myIdeEventQueue.getPopupManager().closeAllPopups(forceRestoreFocus);
  }

  @Override
  public boolean isPopupActive() {
    return myIdeEventQueue.isPopupActive();
  }

  @Override
  public boolean isPopupWindow(Window w) {
    return myIdeEventQueue.getPopupManager().isPopupWindow(w);
  }

  @Override
  public void addDispatcher(Predicate<AWTEvent> dispatcher, @Nullable Disposable parent) {
    myIdeEventQueue.addDispatcher(dispatcher, parent);
  }

  @Override
  public void addPostprocessor(Predicate<AWTEvent> dispatcher, @Nullable Disposable parent) {
    myIdeEventQueue.addPostprocessor(dispatcher, parent);
  }

  @Override
  public boolean containsDispatcher(Predicate<AWTEvent> dispatcher) {
    return myIdeEventQueue.containsDispatcher(dispatcher);
  }

  @Override
  public void removePostprocessor(Predicate<AWTEvent> dispatcher) {
    myIdeEventQueue.removePostprocessor(dispatcher);
  }

  @Override
  public void removeDispatcher(Predicate<AWTEvent> dispatcher) {
    myIdeEventQueue.removeDispatcher(dispatcher);
  }

  @Override
  public void requestFocusInNonFocusedWindow(MouseEvent event) {
    IdeMouseEventDispatcher.requestFocusInNonFocusedWindow(event);
  }

  @Override
  public void blockNextEvents(MouseEvent e) {
    myIdeEventQueue.blockNextEvents(e);
  }

  @Override
  public AWTEvent getTrueCurrentEvent() {
    return myIdeEventQueue.getTrueCurrentEvent();
  }

  @Override
  public void doWhenReady(Runnable runnable) {
    myIdeEventQueue.doWhenReady(runnable);
  }

  @Override
  public void addActivityListener(Runnable runnable, Disposable parentDisposable) {
    myIdeEventQueue.addActivityListener(runnable, parentDisposable);
  }

  @Override
  public void removeActivityListener(Runnable runnable) {
    myIdeEventQueue.removeActivityListener(runnable);
  }

  @Override
  public boolean isKeyEventDispatcherReady() {
    return myIdeEventQueue.getKeyEventDispatcher().isReady();
  }

  @Override
  public void pushPopup(IdePopupEventDispatcher idePopupEventDispatcher) {
    myIdeEventQueue.getPopupManager().push(idePopupEventDispatcher);
  }

  @Override
  public void removePopup(IdePopupEventDispatcher idePopupEventDispatcher) {
    myIdeEventQueue.getPopupManager().remove(idePopupEventDispatcher);
  }

  @Override
  public void disableInputMethods(Disposable parentDisposable) {
    myIdeEventQueue.disableInputMethods(parentDisposable);
  }

  @Override
  public void dispatchEvent(AWTEvent event) {
    myIdeEventQueue.dispatchEvent(event);
  }

  @Override
  public void addIdleListener(Runnable runnable, int timeoutMillis) {
    myIdeEventQueue.addIdleListener(runnable, timeoutMillis);
  }

  @Override
  public void removeIdleListener(Runnable runnable) {
    myIdeEventQueue.removeIdleListener(runnable);
  }
}
