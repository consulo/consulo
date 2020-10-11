// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for running a task in a proper context defined using various builder methods of this class and it's
 * implementations, like [com.intellij.openapi.application.AppUIExecutor.later], or generic [withConstraint].
 * <p>
 * ## Implementation notes: ##
 * <p>
 * The [scheduleWithinConstraints] starts checking the list of constraints, one by one, rescheduling and restarting itself
 * for each unsatisfied constraint ([ReschedulingAttempt]), until at some point *all* of the constraints are satisfied *at once*.
 * <p>
 * This ultimately ends up with [ContextConstraint.schedule] being called one by one for every constraint that needs to be scheduled.
 * Finally, the runnable is called, executing the task in the properly arranged context.
 *
 * @author eldar
 * @author peter
 * <p>
 * from kotlin
 */
public abstract class BaseConstrainedExecution<E extends ConstrainedExecution<E>> implements ConstrainedExecution<E>, ConstrainedExecutionScheduler {
  private static final Logger LOG = Logger.getInstance(BaseConstrainedExecution.class);

  public static class ReschedulingAttempt {
    public static final ReschedulingAttempt NULL = new ReschedulingAttempt(null, null, 0);

    private Stream<ReschedulingAttempt> attemptChain() {
      return Stream.iterate(this, it -> it.previousAttempt != null, it -> it.previousAttempt);
    }

    @Nullable
    private final Object cause;
    @Nullable
    private final ReschedulingAttempt previousAttempt;
    private final int attemptNumber;

    public ReschedulingAttempt(Object cause, ReschedulingAttempt previousAttempt) {
      this(cause, previousAttempt, previousAttempt.attemptNumber + 1);
    }

    public ReschedulingAttempt(@Nullable Object cause, @Nullable ReschedulingAttempt previousAttempt, int attemptNumber) {
      this.cause = cause;
      this.previousAttempt = previousAttempt;
      this.attemptNumber = attemptNumber;

      if (attemptNumber > 3000) {
        String lastCauses = attemptChain().limit(15).map(it -> String.valueOf(it.cause)).collect(Collectors.joining(", "));
        LOG.error("Too many reschedule requests, probably constraints can't be satisfied all together: " + lastCauses);
      }
    }

    @Override
    public String toString() {
      int limit = 5;

      List<String> lastCauses = attemptChain().limit(limit).map(it -> "[" + it.attemptNumber + "]" + it.cause + "").collect(Collectors.toList());

      if (lastCauses.size() == limit) {
        lastCauses.set(limit - 1, "...");
      }

      return StringUtil.join(lastCauses, " <- ");
    }
  }

  public abstract static class RunnableReschedulingAttempt extends ReschedulingAttempt implements Runnable {
    public RunnableReschedulingAttempt(Object cause, ReschedulingAttempt previousAttempt) {
      super(cause, previousAttempt);
    }

    public RunnableReschedulingAttempt(@Nullable Object cause, @Nullable ReschedulingAttempt previousAttempt, int attemptNumber) {
      super(cause, previousAttempt, attemptNumber);
    }
  }

  protected final ContextConstraint[] constraints;

  protected BaseConstrainedExecution(ContextConstraint[] constraints) {
    this.constraints = constraints;
  }

  protected abstract E cloneWith(ContextConstraint[] constraints);

  @Override
  public E withConstraint(ContextConstraint constraint) {
    return cloneWith(ArrayUtil.append(constraints, constraint));
  }

  @Override
  public ConstrainedTaskExecutor asExecutor() {
    return new ConstrainedTaskExecutor(this, composeCancellationCondition(), composeExpiration());
  }

  @Nullable
  protected Expiration composeExpiration() {
    return null;
  }

  @Nullable
  protected BooleanSupplier composeCancellationCondition() {
    return null;
  }

  @Nonnull
  public ContextConstraint[] getConstraints() {
    return constraints;
  }

  @Override
  public void scheduleWithinConstraints(Runnable runnable, @Nullable BooleanSupplier condition) {
    doScheduleWithinConstraints(reschedulingAttempt -> runnable.run(), condition, ReschedulingAttempt.NULL);
  }

  protected void doScheduleWithinConstraints(Consumer<ReschedulingAttempt> task, ReschedulingAttempt previousAttempt) {
    doScheduleWithinConstraints(task, null, previousAttempt);
  }

  protected void doScheduleWithinConstraints(Consumer<ReschedulingAttempt> task, BooleanSupplier condition, ReschedulingAttempt previousAttempt) {
    if (condition != null && !condition.getAsBoolean()) {
      return;
    }

    for (ContextConstraint constraint : constraints) {
      if (!constraint.isCorrectContext()) {
        constraint.schedule(new RunnableReschedulingAttempt(constraint, previousAttempt) {
          @Override
          public void run() {
            LOG.assertTrue(constraint.isCorrectContext());
            doScheduleWithinConstraints(task, condition, this);
          }
        });
        return;
      }
    }

    task.accept(previousAttempt);
  }
}
