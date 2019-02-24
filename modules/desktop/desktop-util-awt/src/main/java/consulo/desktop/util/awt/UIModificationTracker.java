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
package consulo.desktop.util.awt;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author VISTALL
 * @since 17-Jun-17
 */
public class UIModificationTracker {
  public static UIModificationTracker getInstance() {
    return ourInstance;
  }

  private static final UIModificationTracker ourInstance = new UIModificationTracker();


  private static final AtomicIntegerFieldUpdater<UIModificationTracker> UPDATER =
          AtomicIntegerFieldUpdater.newUpdater(UIModificationTracker.class, "myCounter");

  public volatile int myCounter;  // is public to work around JDK-7103570

  public long getModificationCount() {
    return myCounter;
  }

  public void incModificationCount() {
    UPDATER.incrementAndGet(this);
  }
}
