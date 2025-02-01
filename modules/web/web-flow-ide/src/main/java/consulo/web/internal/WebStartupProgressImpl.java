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
package consulo.web.internal;

import consulo.application.internal.StartupProgress;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 08-Oct-17
 */
public class WebStartupProgressImpl implements StartupProgress {
  private int myProgress;
  private String myMessage;

  private final List<Consumer<WebStartupProgressImpl>> myListeners = new SmartList<>();

  @Override
  public void showProgress(String message, float progress) {
    myMessage = message;
    myProgress = (int)(progress * 100);

    for (Consumer<WebStartupProgressImpl> listener : myListeners) {
      listener.accept(this);
    }
  }

  @Override
  public void dispose() {
    myListeners.clear();
  }

  public void addListener(@Nonnull Consumer<WebStartupProgressImpl> consumer) {
    myListeners.add(consumer);
  }

  public String getMessage() {
    return myMessage;
  }

  public int getProgress() {
    return myProgress;
  }
}
