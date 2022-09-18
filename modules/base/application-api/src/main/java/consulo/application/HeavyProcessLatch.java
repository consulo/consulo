// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.application;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Allows tracking some operations as "heavy" and querying their execution status.
 * Typically, some threads call {@link #performOperation} to execute heavy operation (heavy operations can be arbitrarily interleaved).
 * Some other threads then call {@link #isRunning()} and others to query for heavy operations running in background.
 */
public final class HeavyProcessLatch {
  private static final Logger LOG = Logger.getInstance(HeavyProcessLatch.class);

  public static final HeavyProcessLatch INSTANCE = new HeavyProcessLatch();

  private final List<Operation> myHeavyProcesses = Lists.newLockFreeCopyOnWriteList();
  private final EventDispatcher<HeavyProcessListener> myEventDispatcher = EventDispatcher.create(HeavyProcessListener.class);
  private final Queue<Runnable> myExecuteOutOfHeavyActivity = new ConcurrentLinkedQueue<>();

  private HeavyProcessLatch() {
  }

  /**
   * Approximate type of heavy operation. Used in {@link TrafficLightRenderer} UI as brief description.
   */
  public enum Type {
    Indexing("heavyProcess.type.indexing"),
    Syncing("heavyProcess.type.syncing"),
    Processing("heavyProcess.type.processing");

    private final String bundleKey;

    Type(String bundleKey) {
      this.bundleKey = bundleKey;
    }

    @Override
    public String toString() {
      return ApplicationBundle.message(bundleKey);
    }}

  /**
   * @deprecated use {@link #performOperation} instead
   */
  @Deprecated
  @Nonnull
  public AccessToken processStarted(@Nonnull @Nls String displayName) {
    Op op = new Op(Type.Processing, displayName);
    myHeavyProcesses.add(op);
    myEventDispatcher.getMulticaster().processStarted(op);
    return new AccessToken() {
      @Override
      public void finish() {
        myEventDispatcher.getMulticaster().processFinished(op);
        myHeavyProcesses.remove(op);
        executeHandlers();
      }
    };
  }

  /**
   * Executes {@code runnable} as a heavy operation. E.g., during this method execution, {@link #isRunning()} returns true.
   */
  public void performOperation(@Nonnull Type type, @Nonnull @Nls String displayName, @Nonnull Runnable runnable) {
    Op op = new Op(type, displayName);
    myHeavyProcesses.add(op);
    myEventDispatcher.getMulticaster().processStarted(op);
    try {
      runnable.run();
    }
    finally {
      myEventDispatcher.getMulticaster().processFinished(op);
      myHeavyProcesses.remove(op);
      executeHandlers();
    }
  }

  private void executeHandlers() {
    if (!isRunning()) {
      Runnable runnable;
      while ((runnable = myExecuteOutOfHeavyActivity.poll()) != null) {
        try {
          runnable.run();
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
  }

  /**
   * @return {@code true} if some heavy operation is running on some thread
   */
  public boolean isRunning() {
    return !myHeavyProcesses.isEmpty();
  }

  /**
   * @return {@code true} if any heavy operation of type {@code type} is currently running in some thread
   */
  public boolean isRunning(@Nonnull Type type) {
    return ContainerUtil.exists(myHeavyProcesses, op -> op.getType() == type);
  }

  /**
   * @return {@code true} if there is a heavy operation currently running in some thread,
   * which has its {@link Operation#getType()} != {@code type}
   */
  public boolean isRunningAnythingBut(@Nonnull Type type) {
    return ContainerUtil.exists(myHeavyProcesses, op -> op.getType() != type);
  }

  /**heavyProcessIsRunning
   * @return heavy operation currently running, if any, in undefined order
   */
  public Operation getAnyRunningOperation() {
    Iterator<Operation> iterator = myHeavyProcesses.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  /**
   * @return all heavy operations currently running, in undefined order, or an empty collection
   */
  @Nonnull
  public Collection<Operation> getRunningOperations() {
    return new ArrayList<>(myHeavyProcesses);
  }

  @SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
  public interface HeavyProcessListener extends EventListener {
    default void processStarted(@Nonnull Operation op) {
    }

    void processFinished(@Nonnull Operation op);
  }

  public interface Operation {
    @Nonnull
    Type getType();

    @Nonnull
    @Nls
    String getDisplayName();
  }

  public void addListener(@Nonnull Disposable parentDisposable, @Nonnull HeavyProcessListener listener) {
    myEventDispatcher.addListener(listener, parentDisposable);
  }

  /**
   * schedules {@code runnable} to be executed when all heavy operations are finished (i.e., when {@link #isRunning()} returned false)
   */
  public void queueExecuteOutOfHeavyProcess(@Nonnull Runnable runnable) {
    if (isRunning()) {
      myExecuteOutOfHeavyActivity.add(runnable);
    }
    else {
      runnable.run();
    }
  }

  private static final class Op implements Operation {
    private final Type myType;
    private final
    @Nonnull
    @Nls
    String myDisplayName;

    Op(@Nonnull Type type, @Nonnull @Nls String displayName) {
      myType = type;
      myDisplayName = displayName;
    }

    @Override
    public
    @Nonnull
    Type getType() {
      return myType;
    }

    @Override
    public
    @Nls
    @Nonnull
    String getDisplayName() {
      return myDisplayName;
    }
  }
}
