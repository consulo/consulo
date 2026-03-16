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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.ex.popup.IdePopupEventDispatcher;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * Temp proxy - we don't need it in platform code, remove when all awt code moved to awt module
 *
 * @author VISTALL
 * @since 2023-11-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface IdeEventQueueProxy {
  static IdeEventQueueProxy getInstance() {
    return Application.get().getInstance(IdeEventQueueProxy.class);
  }

  default void flushDelayedKeyEvents() {
  }

  default boolean closeAllPopups() {
    return closeAllPopups(true);
  }

  default boolean closeAllPopups(boolean forceRestoreFocus) {
    return false;
  }

  default boolean isPopupActive() {
    return false;
  }

  default boolean isPopupWindow(Window w) {
    return false;
  }

  default void addDispatcher(Predicate<AWTEvent> dispatcher, @Nullable Disposable parent) {
  }

  default void addPostprocessor(Predicate<AWTEvent> dispatcher, @Nullable Disposable parent) {
  }

  default boolean containsDispatcher(Predicate<AWTEvent> dispatcher) {
    return false;
  }

  default void removePostprocessor(Predicate<AWTEvent> dispatcher) {
  }

  default void removeDispatcher(Predicate<AWTEvent> dispatcher) {
  }

  default void requestFocusInNonFocusedWindow(MouseEvent event) {
  }

  default void blockNextEvents(MouseEvent e) {
  }

  default AWTEvent getTrueCurrentEvent() {
    return null;
  }

  default void doWhenReady(Runnable runnable) {
    runnable.run();
  }

  default void addActivityListener(Runnable runnable, Disposable parentDisposable) {
  }

  default void removeActivityListener(Runnable runnable) {
  }

  default boolean isKeyEventDispatcherReady() {
    return true;
  }

  default void pushPopup(IdePopupEventDispatcher idePopupEventDispatcher) {
  }

  default void removePopup(IdePopupEventDispatcher idePopupEventDispatcher) {
  }

  default void disableInputMethods(Disposable parentDisposable) {
  }

  default void dispatchEvent(AWTEvent event) {
  }

  default void addIdleListener(Runnable runnable, int timeoutMillis) {
  }

  default void removeIdleListener(Runnable runnable) {
  }
}
