/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.process;

import consulo.application.util.Semaphore;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseProcessHandler extends UserDataHolderBase implements ProcessHandler {
  private static final Logger LOG = Logger.getInstance(ProcessHandler.class);
  /**
   * todo: replace with an overridable method [nik]
   *
   * @deprecated
   */
  public static final Key<Boolean> SILENTLY_DESTROY_ON_CLOSE = Key.create("SILENTLY_DESTROY_ON_CLOSE");
  public static final Key<Boolean> TERMINATION_REQUESTED = Key.create("TERMINATION_REQUESTED");

  private final List<ProcessListener> myListeners = Lists.newLockFreeCopyOnWriteList();

  private static final int STATE_INITIAL = 0;
  private static final int STATE_RUNNING = 1;
  private static final int STATE_TERMINATING = 2;
  private static final int STATE_TERMINATED = 3;

  private final AtomicInteger myState = new AtomicInteger(STATE_INITIAL);

  private final Semaphore myWaitSemaphore;
  private final ProcessListener myEventMulticaster;
  private final TasksRunner myAfterStartNotifiedRunner;

  @Nullable
  private volatile Integer myExitCode = null;

  private Map<Class, ProcessHandlerFeature> myFeatures;

  protected BaseProcessHandler() {
    myEventMulticaster = createEventMulticaster();
    myWaitSemaphore = new Semaphore();
    myWaitSemaphore.down();
    myAfterStartNotifiedRunner = new TasksRunner();
    myListeners.add(myAfterStartNotifiedRunner);
  }

  @Override
  public void startNotify() {
    if (myState.compareAndSet(STATE_INITIAL, STATE_RUNNING)) {
      myEventMulticaster.startNotified(new ProcessEvent(this));
    }
    else {
      LOG.error("startNotify called already");
    }
  }

  /**
   * @return process handler id
   */
  @Override
  public long getId() {
    // -1 mean not implemented, and not required
    return -1;
  }

  protected abstract void destroyProcessImpl();

  protected abstract void detachProcessImpl();

  @Override
  public abstract boolean detachIsDefault();

  /**
   * Wait for process execution.
   *
   * @return true if target process has actually ended; false if we stopped watching the process execution and don't know if it has completed.
   */
  @Override
  public boolean waitFor() {
    try {
      myWaitSemaphore.waitFor();
      return true;
    }
    catch (ProcessCanceledException e) {
      return false;
    }
  }

  @Override
  public boolean waitFor(long timeoutInMilliseconds) {
    try {
      return myWaitSemaphore.waitFor(timeoutInMilliseconds);
    }
    catch (ProcessCanceledException e) {
      return false;
    }
  }

  @Override
  public void destroyProcess() {
    myAfterStartNotifiedRunner.execute(() -> {
      if (myState.compareAndSet(STATE_RUNNING, STATE_TERMINATING)) {
        fireProcessWillTerminate(true);
        destroyProcessImpl();
      }
    });
  }

  @Override
  public void detachProcess() {
    myAfterStartNotifiedRunner.execute(() -> {
      if (myState.compareAndSet(STATE_RUNNING, STATE_TERMINATING)) {
        fireProcessWillTerminate(false);
        detachProcessImpl();
      }
    });
  }

  @Override
  public boolean isProcessTerminated() {
    return myState.get() == STATE_TERMINATED;
  }

  @Override
  public boolean isProcessTerminating() {
    return myState.get() == STATE_TERMINATING;
  }

  /**
   * @return exit code if the process has already finished, null otherwise
   */
  @Override
  @Nullable
  public Integer getExitCode() {
    return myExitCode;
  }

  @Override
  public void addProcessListener(ProcessListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeProcessListener(ProcessListener listener) {
    myListeners.remove(listener);
  }

  protected void notifyProcessDetached() {
    notifyTerminated(0, false);
  }

  protected void notifyProcessTerminated(int exitCode) {
    notifyTerminated(exitCode, true);
  }

  private void notifyTerminated(int exitCode, boolean willBeDestroyed) {
    myAfterStartNotifiedRunner.execute(() -> {
      LOG.assertTrue(isStartNotified(), "Start notify is not called");

      if (myState.compareAndSet(STATE_RUNNING, STATE_TERMINATING)) {
        try {
          fireProcessWillTerminate(willBeDestroyed);
        }
        catch (Throwable e) {
          if (!isCanceledException(e)) {
            LOG.error(e);
          }
        }
      }

      if (myState.compareAndSet(STATE_TERMINATING, STATE_TERMINATED)) {
        try {
          myExitCode = exitCode;
          myEventMulticaster.processTerminated(new ProcessEvent(BaseProcessHandler.this, exitCode));
        }
        catch (Throwable e) {
          if (!isCanceledException(e)) {
            LOG.error(e);
          }
        }
        finally {
          myWaitSemaphore.up();
        }
      }
    });
  }

  @Override
  public void notifyTextAvailable(String text, Key outputType) {
    ProcessEvent event = new ProcessEvent(this, text);
    myEventMulticaster.onTextAvailable(event, outputType);
  }

  @Override
  @Nullable
  public abstract OutputStream getProcessInput();

  private void fireProcessWillTerminate(boolean willBeDestroyed) {
    LOG.assertTrue(isStartNotified(), "All events should be fired after startNotify is called");
    myEventMulticaster.processWillTerminate(new ProcessEvent(this), willBeDestroyed);
  }

  @Override
  public boolean isStartNotified() {
    return myState.get() > STATE_INITIAL;
  }

  @Override
  public boolean isSilentlyDestroyOnClose() {
    return false;
  }

  protected <F extends ProcessHandlerFeature> void registerFeature(@Nonnull Class<F> featureClass, F feature) {
    Map<Class, ProcessHandlerFeature> features = myFeatures;
    if (features == null) {
      features = myFeatures = new HashMap<>();
    }

    features.put(featureClass, feature);
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <F extends ProcessHandlerFeature> F getFeature(@Nonnull Class<F> featureClass) {
    Map<Class, ProcessHandlerFeature> features = myFeatures;
    if (features == null) {
      return null;
    }
    return (F)features.get(featureClass);
  }

  private ProcessListener createEventMulticaster() {
    Class<ProcessListener> listenerClass = ProcessListener.class;
    return (ProcessListener)Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, new InvocationHandler() {
      @Override
      public Object invoke(Object object, Method method, Object[] params) throws Throwable {
        for (ProcessListener listener : myListeners) {
          try {
            method.invoke(listener, params);
          }
          catch (Throwable e) {
            if (!isCanceledException(e)) {
              LOG.error(e);
            }
          }
        }
        return null;
      }
    });
  }

  private static boolean isCanceledException(Throwable e) {
    boolean value = e instanceof InvocationTargetException && e.getCause() instanceof ProcessCanceledException;
    if (value) {
      LOG.info(e);
    }
    return value;
  }

  private final class TasksRunner implements ProcessListener {
    private final List<Runnable> myPendingTasks = new ArrayList<>();

    @Override
    public void startNotified(ProcessEvent event) {
      removeProcessListener(this);
      // at this point it is guaranteed that nothing will be added to myPendingTasks
      runPendingTasks();
    }

    public void execute(@Nonnull Runnable task) {
      if (isStartNotified()) {
        task.run();
      }
      else {
        synchronized (myPendingTasks) {
          myPendingTasks.add(task);
        }
        if (isStartNotified()) {
          runPendingTasks();
        }
      }
    }

    private void runPendingTasks() {
      Runnable[] tasks;
      synchronized (myPendingTasks) {
        tasks = myPendingTasks.toArray(new Runnable[myPendingTasks.size()]);
        myPendingTasks.clear();
      }
      for (Runnable task : tasks) {
        task.run();
      }
    }
  }
}
