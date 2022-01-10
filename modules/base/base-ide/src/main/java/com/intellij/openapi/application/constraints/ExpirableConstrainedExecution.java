// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import consulo.disposer.Disposable;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * This class adds support for cancelling the task on disposal of [Disposable]s associated using [expireWith] and other builder methods.
 * This also ensures that if the task is a coroutine suspended at some execution point, it's resumed with a [CancellationException] giving
 * the coroutine a chance to clean up any resources it might have acquired before suspending.
 *
 * @author eldar
 */
public abstract class ExpirableConstrainedExecution<E extends ConstrainedExecution<E>> extends BaseConstrainedExecution<E> {
  public static class RunOnce {
    private AtomicBoolean hasNotRunYet = new AtomicBoolean(true);

    public boolean isActive() {
      return hasNotRunYet.get();
    }

    void invoke(Runnable block) {
      if (hasNotRunYet.compareAndSet(true, false)) {
        block.run();
      }
    }
  }

  /**
   * Wraps an expirable context constraint so that the [schedule] method guarantees to execute runnables, regardless the [expiration] state.
   * <p>
   * This is used in combination with execution services that might refuse to run a submitted task due to disposal of an associated
   * Disposable. For example, the DumbService used in [com.intellij.openapi.application.AppUIExecutor.inSmartMode] doesn't run any task once
   * the project is closed. The [ExpirableContextConstraint] workarounds that limitation, ensuring that even if the corresponding disposable
   * is expired, the task runs eventually, which in turn is crucial for Kotlin Coroutines to work properly.
   */
  public class ExpirableContextConstraint implements ContextConstraint {
    private final ContextConstraint constraint;
    private final Expiration expiration;

    public ExpirableContextConstraint(ContextConstraint constraint, Expiration expiration) {
      this.constraint = constraint;
      this.expiration = expiration;
    }

    @Override
    public boolean isCorrectContext() {
      return expiration.isExpired() || constraint.isCorrectContext();
    }

    @Override
    public void schedule(Runnable runnable) {
      RunOnce runOnce = new RunOnce();

      Runnable invokeWhenCompletelyExpiredRunnable = new Runnable() {
        @Override
        public void run() {
          if (expiration.isExpired()) {
            runOnce.invoke(() -> {
              // At this point all the expiration handlers, including the one responsible for cancelling the coroutine job, have finished.
              runnable.run();
            });
          }
          else if (runOnce.isActive()) {
            dispatchLaterUnconstrained(this);
          }
        }
      };

      Expiration.Handle expirationHandle = expiration.invokeOnExpiration(invokeWhenCompletelyExpiredRunnable);

      if (runOnce.isActive()) {
        constraint.schedule(() -> {
          runOnce.invoke(() -> {
            expirationHandle.unregisterHandler();

            runnable.run();
          });
        });
      }
    }
  }

  protected final BooleanSupplier[] cancellationConditions;
  protected final Set<? extends Expiration> expirationSet;

  public ExpirableConstrainedExecution(ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirationSet) {
    super(constraints);
    this.cancellationConditions = cancellationConditions;
    this.expirationSet = expirationSet;
  }

  @Override
  protected E cloneWith(ContextConstraint[] constraints) {
    return cloneWith(constraints, cancellationConditions, expirationSet);
  }

  @Nonnull
  protected abstract E cloneWith(ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirationSet);

  @Override
  @Nonnull
  public E withConstraint(ContextConstraint constraint, Disposable parentDisposable) {
    Expiration expirableHandle = new DisposableExpiration(parentDisposable);
    ContextConstraint expirableConstraint = new ExpirableContextConstraint(constraint, expirableHandle);

    return cloneWith(ArrayUtil.append(constraints, expirableConstraint), cancellationConditions, appendSet(this.expirationSet, expirableHandle));
  }

  private final NullableLazyValue<Expiration> compositeExpiration = NullableLazyValue.createValue(() -> ExpirationUtil.composeExpiration(getExpirationSet()));

  @Nullable
  @Override
  protected Expiration composeExpiration() {
    return compositeExpiration.getValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public E expireWith(Disposable parentDisposable) {
    Expiration expirableHandle = new DisposableExpiration(parentDisposable);
    if (expirationSet.contains(expirableHandle)) {
      return (E)this;
    }

    return cloneWith(constraints, cancellationConditions, appendSet(expirationSet, expirableHandle));
  }

  @Override
  public E cancelIf(BooleanSupplier condition) {
    return cloneWith(constraints, ArrayUtil.append(cancellationConditions, condition), expirationSet);
  }

  public abstract void dispatchLaterUnconstrained(Runnable runnable);

  public BooleanSupplier[] getCancellationConditions() {
    return cancellationConditions;
  }

  public Set<? extends Expiration> getExpirationSet() {
    return expirationSet;
  }

  @Nullable
  @Override
  protected BooleanSupplier composeCancellationCondition() {
    BooleanSupplier[] conditions = this.cancellationConditions;
    if (conditions.length == 0) {
      return null;
    }
    else if (conditions.length == 1) {
      return conditions[0];
    }
    else {
      return () -> {
        for (BooleanSupplier condition : conditions) {
          if (condition.getAsBoolean()) {
            return true;
          }
        }

        return false;
      };
    }
  }

  @Nonnull
  private static <T> Set<T> appendSet(Set<? extends T> set, T value) {
    Set<T> newSet = new HashSet<>(set);
    newSet.add(value);
    return newSet;
  }
}
