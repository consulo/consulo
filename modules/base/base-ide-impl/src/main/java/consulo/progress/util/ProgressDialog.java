/*
 * Copyright 2013-2020 consulo.io
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
package consulo.progress.util;

import consulo.disposer.Disposable;
import javax.annotation.Nonnull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public interface ProgressDialog extends Disposable {
  void startBlocking(@Nonnull CompletableFuture<?> stopCondition, @Nonnull Predicate<AWTEvent> isCancellationEvent);

  void hide();

  default void hideImmediately() {
    hide();
  }

  void background();

  void update();

  default void cancel() {
    enableCancelButtonIfNeeded(false);
  }

  void show();

  void runRepaintRunnable();

  void changeCancelButtonText(String text);

  void enableCancelButtonIfNeeded(boolean value);

  boolean isPopupWasShown();

  default void copyPopupStateToWindow() {
    // fixme [vistall] just hack for desktop
  }
}
