// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.animation;

import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.ui.RemoteDesktopService;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.UIAccessScheduler;
import consulo.util.lang.MathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Animator schedules and runs animations.</p>
 *
 * <p>The total duration of one call of {@link #animate(Collection)} depends on
 * summary duration of all submitted animations.</p>
 *
 * <p>Let's assume that next animations were submitted:</p>
 *
 * <pre>
 * a1[delay = 10, duration = 40]
 * a2[delay = 50, duration = 20]
 * a3[delay = 40, duration = 50]
 * </pre>
 *
 * <p>The total duration time is: <code>maxOf(10 + 40, 50 + 20, 40 + 50) = 90</code>,
 * the delay before running first animation cycle is <code>minOf(10, 50, 40) = 10</code>.</p>
 *
 * <p>There are no guarantees that any animation starts exactly in time,
 * but it runs as soon as possible. For example when the period is set to 15 and total delay is 0
 * then an animation with delay = 20 will be played at 30 (on the second cycle).
 * </p>
 *
 * <p>
 * The simplest way to run an animation:
 *
 * <pre>
 *     new JBAnimator().animate(
 *       new Animation((v) -> System.out.println(v))
 *     );
 *   </pre>
 * </p>
 *
 * <p>
 * By default it runs on EDT with duration 500 ms.
 * </p>
 *
 * @see Animation
 * @see Animations
 */
public final class JBAnimator implements Disposable {

  private int myPeriod = 16;
  private @Nonnull
  Type myType = Type.IN_TIME;
  private boolean myCyclic = false;
  private boolean myIgnorePowerSaveMode = false;
  private
  @Nullable
  String myName = null;

  private final @Nonnull
  ScheduledExecutorService myService;
  private final @Nonnull
  AtomicLong myRunning = new AtomicLong();
  private final @Nonnull
  AtomicBoolean myDisposed = new AtomicBoolean();

  private static final Logger LOG = Logger.getInstance(JBAnimator.class);
  private
  @Nullable
  Statistic myStatistic = null;

  public JBAnimator() {
    this(Thread.SWING_THREAD, null);
  }

  @SuppressWarnings("unused")
  public JBAnimator(@Nonnull Disposable parentDisposable) {
    this(Thread.SWING_THREAD, parentDisposable);
  }

  public JBAnimator(@Nonnull Thread threadToUse, @Nullable Disposable parentDisposable) {
    myService = threadToUse == Thread.SWING_THREAD ?
      Application.get().getLastUIAccess().getScheduler() :
      AppExecutorUtil.createBoundedScheduledExecutorService("Animator Pool", 1);
    if (parentDisposable == null) {
      if (threadToUse != Thread.SWING_THREAD) {
        Logger.getInstance(JBAnimator.class)
              .error(new IllegalArgumentException("You must provide parent Disposable for non-swing thread Alarm"));
      }
    }
    else {
      Disposer.register(parentDisposable, this);
    }
  }

  /**
   * @see #animate(Collection)
   */
  public long animate(@Nonnull  Animation... animations) {
    return animate(Arrays.asList(animations));
  }

  /**
   * Runs collection of animations.
   * <p>
   * If animator is running then stop previous submitted animations
   * and schedule new.
   *
   * @param animations Collection of animations to be scheduled for running.
   * @return task ID or {@link Long#MAX_VALUE} if animator is disposed
   */
  public long animate(@Nonnull Collection< Animation> animations) {
    if (myDisposed.get()) {
      LOG.warn("Animator is already disposed");
      return Long.MAX_VALUE;
    }

    var from = Integer.MAX_VALUE;
    var to = 0;

    for (Animation animation : animations) {
      from = Math.min(animation.getDelay(), from);
      to = Math.max(animation.getDelay() + animation.getDuration(), to);
    }

    final var delay = animations.isEmpty() ? 0 : from;
    final var duration = animations.isEmpty() ? 0 : to - from;

    final var taskId = myRunning.incrementAndGet();

    if (!myIgnorePowerSaveMode && PowerSaveMode.isEnabled()
      || Registry.is("ui.no.bangs.and.whistles")
      || RemoteDesktopService.isRemoteSession()
      || duration == 0) {
      myService.schedule(() -> {
        if (taskId < myRunning.get()) {
          for (Animation animation : animations) {
            animation.fireEvent(Animation.Phase.CANCELLED);
          }
          return;
        }
        for (Animation animation : animations) {
          try {
            animation.fireEvent(Animation.Phase.SCHEDULED);
            animation.update(1.0);
            animation.fireEvent(Animation.Phase.UPDATED);
            animation.fireEvent(Animation.Phase.EXPIRED);
          }
          catch (Throwable t) {
            LOG.error(t);
          }
        }
        myRunning.compareAndSet(taskId, taskId + 1);
      }, delay, TimeUnit.MILLISECONDS);
      return taskId;
    }

    final var stat = new Statistic(myName, taskId);
    stat.start = System.nanoTime();
    if (myPeriod < 16) { // do not enable this until it's really necessary
      JBAnimatorHelper.requestHighPrecisionTimer(this);
    }

    myService.schedule(new Runnable() {
      final Type type = myType;
      final int period = myPeriod;
      final boolean cycle = myCyclic;
      @Nullable
      FrameCounter frameCounter;
      @Nonnull
      LinkedHashSet<Animation> scheduledAnimations = new LinkedHashSet<>();
      //private final long animationStarted = System.nanoTime();
      private long nextScheduleTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delay); // ns

      private void prepareAnimations() {
        frameCounter = create(type, period, duration);
        scheduledAnimations = new LinkedHashSet<>(animations);
        for (Animation animation : scheduledAnimations) {
          animation.fireEvent(Animation.Phase.SCHEDULED);
        }
      }

      private void finalizeRunning() {
        JBAnimatorHelper.cancelHighPrecisionTimer(JBAnimator.this);
        stat.end = System.nanoTime();
        myStatistic = stat;
      }

      @Override
      public void run() {
        stat.count.incrementAndGet();
        // There's a penalty for run a task.
        // To decrease cumulative penalty difference between real start
        // and expected is subtracted from the next delay
        long wasLate = System.nanoTime() - nextScheduleTime;
        if (wasLate < 0) {
          LOG.warn("Negative animation late value");
          wasLate = 0;
        }
        if (taskId < myRunning.get()) {
          finalizeRunning();
          for (Animation animation : scheduledAnimations) {
            animation.fireEvent(Animation.Phase.CANCELLED);
          }
          return;
        }
        if (frameCounter == null) {
          prepareAnimations();
        }
        long totalFrames = frameCounter.getTotalFrames();
        long currentFrame = Math.min(frameCounter.getNextFrame(cycle), totalFrames);
        long currentDelay = frameCounter.getDelay(currentFrame);
        double timeline = (double)currentFrame / totalFrames;
        if (currentFrame >= totalFrames && cycle) {
          frameCounter = null;
        }
        var expired = new LinkedList<Animation>();
        for (Animation animation : scheduledAnimations) {
          double start = (double)(animation.getDelay() - delay) / duration;
          double end = start + (double)animation.getDuration() / duration;
          if (start <= timeline) try {
            double current = (timeline - start) / (end - start);
            animation.update(MathUtil.clamp(current, 0.0, 1.0));
            animation.fireEvent(Animation.Phase.UPDATED);
          }
          catch (Throwable t) {
            LOG.error(t);
          }
          if (timeline > end) {
            expired.add(animation);
          }
        }
        expired.forEach(scheduledAnimations::remove);
        boolean isProceed = currentFrame < totalFrames || cycle;
        if (isProceed) {
          long nextDelay = Math.max(TimeUnit.MILLISECONDS.toNanos(currentDelay) - wasLate, TimeUnit.MILLISECONDS.toNanos(1));
          nextScheduleTime = System.nanoTime() + nextDelay;
          myService.schedule(this, nextDelay, TimeUnit.NANOSECONDS);
        }
        else {
          // There's a situation when a new task is submitted but current is already in progress.
          // For example, the task can be submitted with Animation#runWhenExpired,
          // but this code synchronously can fire animate, therefore myRunning increases.
          // If this situation happens the current one ID is abandoned,
          // because the value is increased somewhere else.
          myRunning.compareAndSet(taskId, taskId + 1);
          //var debugInfo = "Animation total time is " +
          //                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - animationStarted) +
          //                " ms; requested time is " +
          //                (delay + duration);
          //LOG.info(debugInfo);
          finalizeRunning();
        }
        // we should fire events after taskId is updated
        // and some final activity is done by calling finalizeRunning
        for (Animation animation : isProceed ? expired : scheduledAnimations) {
          animation.fireEvent(Animation.Phase.EXPIRED);
        }
      }
    }, delay, TimeUnit.MILLISECONDS);

    return taskId;
  }

  /**
   * Return if current task will be started in the next cycle.
   * <p>
   * Because any animation cannot be finished instantly
   * isRunning can return <code>false</code> when animation is in process.
   * <p>
   * Animator uses a single-threaded pool therefore it is OK
   * that any new animation is submitted. It starts after current animation is over.
   *
   * @param taskId id that is given when {@link #animate(Collection)} is called
   * @return true if the animation will be started next animation cycle.
   */
  public boolean isRunning(long taskId) {
    return myRunning.get() == taskId;
  }

  /**
   * Stops all submitted animations in the next animation cycle.
   */
  public void stop() {
    myRunning.incrementAndGet();
  }

  public int getPeriod() {
    return myPeriod;
  }

  public @Nonnull
  JBAnimator setPeriod(int period) {
    myPeriod = Math.max(period, 1);
    return this;
  }

  public boolean isCyclic() {
    return myCyclic;
  }

  /**
   * Set flag to repeat animations. Has no effect if animator is running.
   * <p>
   * When set in true there's no value 1.0 for {@link Type#EACH_FRAME} mode,
   * so a cyclic animation can be start, e.g. icon animation. For 8 icons
   * there are 8 values will be submitted: 0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875.
   */
  public @Nonnull
  JBAnimator setCyclic(boolean cyclic) {
    myCyclic = cyclic;
    return this;
  }

  public @Nonnull
  JBAnimator ignorePowerSaveMode() {
    myIgnorePowerSaveMode = true;
    return this;
  }

  public @Nonnull
  Type getType() {
    return myType;
  }

  public @Nonnull
  JBAnimator setType(Type type) {
    myType = type;
    return this;
  }

  public
  @Nullable
  String getName() {
    return myName;
  }

  /**
   * Set an optional name to identify the animator.
   */
  public @Nonnull
  JBAnimator setName(@Nullable String name) {
    myName = name;
    return this;
  }

  /**
   * @return statistic of the last animation
   */
  public
  @Nullable
  Statistic getStatistic() {
    return myStatistic;
  }

  @Override
  public void dispose() {
    stop();

    if (!myDisposed.getAndSet(true) && !(myService instanceof UIAccessScheduler)) {
      myService.shutdownNow();
      JBAnimatorHelper.cancelHighPrecisionTimer(this);
    }
  }

  /**
   * <p>The thread where {@link Animation#update(double)} and {@link Animation.Listener#update(Animation.Phase)} will be called.</p>
   */
  public enum Thread {
    SWING_THREAD,
    POOLED_THREAD
  }

  /**
   * <p>Animation can be played 2 different ways:</p>
   *
   * <ul>
   * <li>Every frame of the animation will be played but total duration of the animation can be longer.</li>
   * <li>Some frames of animation can be missed but total duration will be close to demanded.</li>
   * </ul>
   */
  public enum Type {
    /**
     * Animation creates necessary amount of frames and tries to play them.
     *
     * <p>For simple animation n + 1 frame is submitted, started from the 0.0 until 1.0,
     * except the case when animation is cyclic. In the latter case instead of 1.0
     * it starts from 0.0 again.</p>
     *
     * <p>The total time can be considerably greater but all frames will be played.</p>
     *
     * @see #setCyclic(boolean)
     */
    EACH_FRAME,

    /**
     * Animation depends on current system time and plays as close to {@link Animation#getDuration()} as possible.
     *
     * <p>Unlike {@link #EACH_FRAME} can miss some frames or even submit the only last value 1.0.</p>
     */
    IN_TIME,
  }

  private interface FrameCounter {
    long getNextFrame(boolean isCyclic);

    long getTotalFrames();

    long getDelay(long currentFrame);
  }

  private static FrameCounter create(@Nonnull Type type, int period, int duration) {
    return switch(type){
      case EACH_FRAME -> new FrameCounter() {

        final long frames = duration / period + ((duration % period == 0) ? 0 : 1);
        long frame = 0;

        @Override
        public long getNextFrame(boolean isCyclic) {
          var f = frame;
          frame++;
          if (isCyclic) {
            frame %= frames;
          }
          return f;
        }

        @Override
        public long getTotalFrames() {
          // at least one frame should be played
          return Math.max(frames, 1);
        }

        @Override
        public long getDelay(long currentFrame) {
          return period;
        }
      };
      case IN_TIME -> new FrameCounter() {

        final long startTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

        @Override
        public long getNextFrame(boolean isCyclic) {
          return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - startTime;
        }

        @Override
        public long getTotalFrames() {
          return duration;
        }

        @Override
        public long getDelay(long currentFrame) {
          return Math.min(duration - currentFrame, period);
        }
      };
    };
  }

  public static class Statistic {
    private
    @Nullable
    final String myName;
    private final AtomicLong count = new AtomicLong(0);
    private long start;
    private long end;
    private final long taskId;

    public Statistic(@Nullable String name, long id) {
      myName = name;
      taskId = id;
    }

    public long getTaskId() {
      return taskId;
    }

    /**
     * @return total number of frames
     */
    public long getCount() {
      return count.get();
    }

    /**
     * @return animation duration in milliseconds
     */
    public long getDuration() {
      return TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    @Override
    public String toString() {
      return "Statistic{" +
        "name=" + myName +
        ", taskId=" + taskId +
        ", duration=" + getDuration() + "ms" +
        ", count=" + count +
        ", updates=" + (count.get() * 1000 / getDuration()) +
        '}';
    }
  }
}
