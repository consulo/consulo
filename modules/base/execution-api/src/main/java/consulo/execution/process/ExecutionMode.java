/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.execution.process;

import consulo.process.event.ProcessListener;
import consulo.util.collection.Lists;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.function.Function;

/**
* @author Roman.Chernyatchik
*/
public class ExecutionMode {
  private final boolean myCancelable;
  private final String myTitle;
  private final String myTitle2;
  private final boolean myRunWithModal;
  private final boolean myRunInBG;
  private final JComponent myProgressParentComponent;
  private Function<Object, Boolean> myShouldCancelFun;
  private final Object CANCEL_FUN_LOCK = new Object();
  private final List<ProcessListener> myListeners = Lists.newLockFreeCopyOnWriteList();

  public ExecutionMode(final boolean cancelable,
                       @Nullable final String title,
                       @Nullable final String title2,
                       final boolean runInBG,
                       final boolean runWithModal,
                       JComponent progressParentComponent) {
    myCancelable = cancelable;
    myTitle = title;
    myTitle2 = title2;
    myRunInBG = runInBG;
    myRunWithModal = runWithModal;
    myProgressParentComponent = progressParentComponent;
  }

  public int getTimeout() {
    // it is ignored
    return -1;
  }

  @Nullable
  public String getTitle() {
    return myTitle;
  }

  @Nullable
  public String getTitle2() {
    return myTitle2;
  }

  public boolean cancelable() {
    return myCancelable;
  }

  public boolean inBackGround() {
    return myRunInBG;
  }

  public boolean withModalProgress() {
    return myRunWithModal;
  }

  public JComponent getProgressParentComponent() {
    return myProgressParentComponent;
  }

  /**
   * Runner checks this fun during process running, if returns true, process will be canceled.
   */
  @Nullable
  public Function<Object, Boolean> shouldCancelFun() {
    synchronized (CANCEL_FUN_LOCK) {
      return myShouldCancelFun;
    }
  }

  public void setShouldCancelFun(final Function<Object, Boolean> shouldCancelFun) {
    synchronized (CANCEL_FUN_LOCK) {
      myShouldCancelFun = shouldCancelFun;
    }
  }

  public void addProcessListener(@Nonnull final ProcessListener listener) {
    myListeners.add(listener);
  }

  @Nonnull
  public List<ProcessListener> getProcessListeners() {
    return myListeners;
  }
}
