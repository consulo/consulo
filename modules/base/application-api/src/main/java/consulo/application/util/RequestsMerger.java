/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application.util;

import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * For exactly same refresh requests buffering:
 * <p/>
 * - refresh requests can be merged into one, but general principle is that each request should be reliably followed by refresh action
 * - at the moment only one refresh action is being done
 * - if request had been submitted while refresh action was in progress, new refresh action is initiated right after first refresh action finishes
 */
public class RequestsMerger {
  private static final Logger LOG = Logger.getInstance(RequestsMerger.class);

  private final MyWorker myWorker;

  private final Object myLock = new Object();

  private MyState myState;
  private final Consumer<? super Runnable> myAlarm;

  private final List<Runnable> myWaitingStartListeners = new ArrayList<>();
  private final List<Runnable> myWaitingFinishListeners = new ArrayList<>();

  public RequestsMerger(Runnable runnable, Consumer<? super Runnable> alarm) {
    myAlarm = alarm;
    myWorker = new MyWorker(runnable);

    myState = MyState.EMPTY;
  }

  public void request() {
    LOG.debug("ext: request");
    doAction(MyAction.REQUEST);
  }

  public boolean isEmpty() {
    synchronized (myLock) {
      return MyState.EMPTY.equals(myState);
    }
  }

  public void waitRefresh(Runnable runnable) {
    LOG.debug("ext: wait refresh");
    synchronized (myLock) {
      myWaitingStartListeners.add(runnable);
    }
    request();
  }

  private class MyWorker implements Runnable {
    private volatile boolean myInitialized;
    private final Runnable myRunnable;

    private MyWorker(Runnable runnable) {
      myRunnable = runnable;
    }

    @Override
    public void run() {
      LOG.debug("worker: started refresh");
      try {
        doAction(MyAction.START);
        myRunnable.run();
        myInitialized = true;
      }
      finally {
        doAction(MyAction.FINISH);
      }
    }

    public boolean isInitialized() {
      return myInitialized;
    }
  }

  private void doAction(MyAction action) {
    LOG.debug("doAction: START " + action.name());
    MyExitAction[] exitActions;
    List<Runnable> toBeCalled = null;
    synchronized (myLock) {
      MyState oldState = myState;
      myState = myState.transition(action);
      if (oldState.equals(myState)) {
        return;
      }
      exitActions = MyTransitionAction.getExit(oldState, myState);

      LOG.debug("doAction: oldState: " + oldState.name() + ", newState: " + myState.name());

      if (LOG.isDebugEnabled() && exitActions != null) {
        String debugExitActions = StringUtil.join(exitActions, Enum::name, " ");
        LOG.debug("exit actions: " + debugExitActions);
      }
      if (exitActions != null) {
        for (MyExitAction exitAction : exitActions) {
          if (MyExitAction.MARK_START.equals(exitAction)) {
            myWaitingFinishListeners.addAll(myWaitingStartListeners);
            myWaitingStartListeners.clear();
          }
          else if (MyExitAction.MARK_END.equals(exitAction)) {
            toBeCalled = new ArrayList<>(myWaitingFinishListeners);
            myWaitingFinishListeners.clear();
          }
        }
      }
    }
    if (exitActions != null) {
      for (MyExitAction exitAction : exitActions) {
        if (MyExitAction.SUBMIT_REQUEST_TO_EXECUTOR.equals(exitAction)) {
          myAlarm.accept(myWorker);
          //myAlarm.addRequest(myWorker, ourDelay);
          //ApplicationManager.getApplication().executeOnPooledThread(myWorker);
        }
      }
    }
    if (toBeCalled != null) {
      for (Runnable runnable : toBeCalled) {
        runnable.run();
      }
    }
    LOG.debug("doAction: END " + action.name());
  }

  private enum MyState {
    EMPTY() {
      @Override
      public MyState transition(MyAction action) {
        if (MyAction.REQUEST.equals(action)) {
          return MyState.REQUEST_SUBMITTED;
        }
        logWrongAction(this, action);
        return this;
      }
    },
    IN_PROGRESS() {
      @Override
      public MyState transition(MyAction action) {
        if (MyAction.FINISH.equals(action)) {
          return EMPTY;
        }
        else if (MyAction.REQUEST.equals(action)) {
          return MyState.IN_PROGRESS_REQUEST_SUBMITTED;
        }
        logWrongAction(this, action);
        return this;
      }
    },
    IN_PROGRESS_REQUEST_SUBMITTED() {
      @Override
      public MyState transition(MyAction action) {
        if (MyAction.FINISH.equals(action)) {
          return MyState.REQUEST_SUBMITTED;
        }
        if (MyAction.START.equals(action)) {
          logWrongAction(this, action);
        }
        return this;
      }
    },
    REQUEST_SUBMITTED() {
      @Override
      public MyState transition(MyAction action) {
        if (MyAction.START.equals(action)) {
          return IN_PROGRESS;
        }
        else if (MyAction.FINISH.equals(action)) {
          // to be able to be started by another request
          logWrongAction(this, action);
          return EMPTY;
        }
        return this;
      }
    };

    // under lock
    
    public abstract MyState transition(MyAction action);

    private static void logWrongAction(MyState state, MyAction action) {
      LOG.info("Wrong action: state=" + state.name() + ", action=" + action.name());
    }
  }

  private record Transition(MyState from, MyState to) {
  }

  private static class MyTransitionAction {
    private static final Map<Transition, MyExitAction[]> myMap = new HashMap<>();

    static {
      add(MyState.EMPTY, MyState.REQUEST_SUBMITTED, MyExitAction.SUBMIT_REQUEST_TO_EXECUTOR);
      add(MyState.REQUEST_SUBMITTED, MyState.IN_PROGRESS, MyExitAction.MARK_START);
      add(MyState.IN_PROGRESS, MyState.EMPTY, MyExitAction.MARK_END);
      add(MyState.IN_PROGRESS_REQUEST_SUBMITTED, MyState.REQUEST_SUBMITTED, MyExitAction.SUBMIT_REQUEST_TO_EXECUTOR, MyExitAction.MARK_END);

      //... and not real but to be safe:
      add(MyState.IN_PROGRESS_REQUEST_SUBMITTED, MyState.EMPTY, MyExitAction.MARK_END);
      add(MyState.IN_PROGRESS, MyState.REQUEST_SUBMITTED, MyExitAction.MARK_END);
    }

    private static void add(MyState from, MyState to, MyExitAction... action) {
      myMap.put(new Transition(from, to), action);
    }

    public static MyExitAction @Nullable [] getExit(MyState from, MyState to) {
      return myMap.get(new Transition(from, to));
    }
  }

  private enum MyExitAction {
    EMPTY,
    SUBMIT_REQUEST_TO_EXECUTOR,
    MARK_START,
    MARK_END
  }

  private enum MyAction {
    REQUEST,
    START,
    FINISH
  }
}
