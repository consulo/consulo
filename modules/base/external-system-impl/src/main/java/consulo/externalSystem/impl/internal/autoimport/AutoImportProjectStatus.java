// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static consulo.externalSystem.autoimport.ExternalSystemModificationType.EXTERNAL;
import static consulo.externalSystem.autoimport.ExternalSystemModificationType.HIDDEN;
import static consulo.externalSystem.autoimport.ExternalSystemModificationType.INTERNAL;
import static consulo.externalSystem.autoimport.ExternalSystemModificationType.UNKNOWN;

public class AutoImportProjectStatus {
    private static final Logger LOG = Logger.getInstance("#consulo.externalSystem.autoimport");

    private final @Nullable String myDebugName;

    private final AtomicReference<ProjectState> myState = new AtomicReference<>(new ProjectState.Synchronized(Stamp.NONE));

    public AutoImportProjectStatus() {
        this(null);
    }

    public AutoImportProjectStatus(@Nullable String debugName) {
        myDebugName = debugName;
    }

    public boolean isDirty() {
        return myState.get() instanceof ProjectState.Dirty;
    }

    public boolean isUpToDate() {
        return switch (myState.get()) {
            case ProjectState.Modified modified -> false;
            case ProjectState.Dirty dirty -> false;
            case ProjectState.Broken broken -> false;
            case ProjectState.Synchronized synchronizedState -> true;
            case ProjectState.Reverted reverted -> true;
        };
    }

    public ExternalSystemModificationType getModificationType() {
        ProjectState state = myState.get();
        if (state instanceof ProjectState.Dirty dirty) {
            return dirty.type();
        }
        if (state instanceof ProjectState.Modified modified) {
            return modified.type();
        }
        return UNKNOWN;
    }

    public ProjectState markBroken(Stamp stamp) {
        return update(new ProjectEvent.Break(stamp));
    }

    public ProjectState markDirty(Stamp stamp) {
        return markDirty(stamp, INTERNAL);
    }

    public ProjectState markDirty(Stamp stamp, ExternalSystemModificationType type) {
        return update(new ProjectEvent.Invalidate(stamp, type));
    }

    public ProjectState markModified(Stamp stamp) {
        return markModified(stamp, INTERNAL);
    }

    public ProjectState markModified(Stamp stamp, ExternalSystemModificationType type) {
        return update(new ProjectEvent.Modify(stamp, type));
    }

    public ProjectState markReverted(Stamp stamp) {
        return update(new ProjectEvent.Revert(stamp));
    }

    public ProjectState markSynchronized(Stamp stamp) {
        return update(new ProjectEvent.Synchronize(stamp));
    }

    public ProjectState update(ProjectEvent event) {
        AtomicReference<ProjectState> oldState = new AtomicReference<>();
        ProjectState newState = myState.updateAndGet(currentState -> {
            oldState.set(currentState);
            return switch (currentState) {
                case ProjectState.Synchronized state -> switch (event) {
                    case ProjectEvent.Synchronize e -> withFuture(e, state, ProjectState.Synchronized::new);
                    case ProjectEvent.Invalidate e -> ifFuture(e, state, it -> new ProjectState.Dirty(it, e.type()));
                    case ProjectEvent.Modify e -> ifFuture(e, state, it -> new ProjectState.Modified(it, e.type()));
                    case ProjectEvent.Revert e -> ifFuture(e, state, ProjectState.Reverted::new);
                    case ProjectEvent.Break e -> ifFuture(e, state, ProjectState.Broken::new);
                };
                case ProjectState.Dirty state -> switch (event) {
                    case ProjectEvent.Synchronize e -> ifFuture(e, state, ProjectState.Synchronized::new);
                    case ProjectEvent.Invalidate e -> withFuture(e, state, it -> new ProjectState.Dirty(it, merge(state.type(), e.type())));
                    case ProjectEvent.Modify e -> withFuture(e, state, it -> new ProjectState.Dirty(it, merge(state.type(), e.type())));
                    case ProjectEvent.Revert e -> withFuture(e, state, it -> new ProjectState.Dirty(it, state.type()));
                    case ProjectEvent.Break e -> withFuture(e, state, it -> new ProjectState.Dirty(it, state.type()));
                };
                case ProjectState.Modified state -> switch (event) {
                    case ProjectEvent.Synchronize e -> ifFuture(e, state, ProjectState.Synchronized::new);
                    case ProjectEvent.Invalidate e -> withFuture(e, state, it -> new ProjectState.Dirty(it, merge(state.type(), e.type())));
                    case ProjectEvent.Modify e -> withFuture(e, state, it -> new ProjectState.Modified(it, merge(state.type(), e.type())));
                    case ProjectEvent.Revert e -> ifFuture(e, state, ProjectState.Reverted::new);
                    case ProjectEvent.Break e -> withFuture(e, state, it -> new ProjectState.Dirty(it, state.type()));
                };
                case ProjectState.Reverted state -> switch (event) {
                    case ProjectEvent.Synchronize e -> ifFuture(e, state, ProjectState.Synchronized::new);
                    case ProjectEvent.Invalidate e -> withFuture(e, state, it -> new ProjectState.Dirty(it, e.type()));
                    case ProjectEvent.Modify e -> ifFuture(e, state, it -> new ProjectState.Modified(it, e.type()));
                    case ProjectEvent.Revert e -> withFuture(e, state, ProjectState.Reverted::new);
                    case ProjectEvent.Break e -> ifFuture(e, state, ProjectState.Broken::new);
                };
                case ProjectState.Broken state -> switch (event) {
                    case ProjectEvent.Synchronize e -> ifFuture(e, state, ProjectState.Synchronized::new);
                    case ProjectEvent.Invalidate e -> withFuture(e, state, it -> new ProjectState.Dirty(it, e.type()));
                    case ProjectEvent.Modify e -> withFuture(e, state, it -> new ProjectState.Dirty(it, e.type()));
                    case ProjectEvent.Revert e -> withFuture(e, state, ProjectState.Broken::new);
                    case ProjectEvent.Break e -> withFuture(e, state, ProjectState.Broken::new);
                };
            };
        });
        debug(newState, oldState.get(), event);
        return newState;
    }

    private void debug(ProjectState newState, ProjectState oldState, ProjectEvent event) {
        if (LOG.isDebugEnabled()) {
            String debugPrefix = myDebugName == null ? "" : myDebugName + ": ";
            LOG.debug(debugPrefix + oldState + " -> " + newState + " by " + event);
        }
    }

    @Override
    public String toString() {
        String debugPrefix = myDebugName == null ? "" : myDebugName + ": ";
        return debugPrefix + myState.get();
    }

    private static ProjectState withFuture(ProjectEvent event, ProjectState state, Function<Stamp, ProjectState> action) {
        return action.apply(event.stamp().compareTo(state.stamp()) >= 0 ? event.stamp() : state.stamp());
    }

    private static ProjectState ifFuture(ProjectEvent event, ProjectState state, Function<Stamp, ProjectState> action) {
        return event.stamp().compareTo(state.stamp()) > 0 ? action.apply(event.stamp()) : state;
    }

    public static ExternalSystemModificationType merge(ExternalSystemModificationType type1, ExternalSystemModificationType type2) {
        return switch (type1) {
            case INTERNAL -> INTERNAL;
            case EXTERNAL -> switch (type2) {
                case INTERNAL -> INTERNAL;
                case EXTERNAL -> EXTERNAL;
                case HIDDEN -> EXTERNAL;
                case UNKNOWN -> EXTERNAL;
            };
            case HIDDEN -> switch (type2) {
                case INTERNAL -> INTERNAL;
                case EXTERNAL -> EXTERNAL;
                case HIDDEN -> HIDDEN;
                case UNKNOWN -> HIDDEN;
            };
            case UNKNOWN -> type2;
        };
    }

    public sealed interface ProjectEvent {
        Stamp stamp();

        record Synchronize(Stamp stamp) implements ProjectEvent {
        }

        record Invalidate(Stamp stamp, ExternalSystemModificationType type) implements ProjectEvent {
        }

        record Modify(Stamp stamp, ExternalSystemModificationType type) implements ProjectEvent {
        }

        record Revert(Stamp stamp) implements ProjectEvent {
        }

        record Break(Stamp stamp) implements ProjectEvent {
        }

        static ProjectEvent externalModify(Stamp stamp) {
            return new Modify(stamp, EXTERNAL);
        }

        static ProjectEvent externalInvalidate(Stamp stamp) {
            return new Invalidate(stamp, EXTERNAL);
        }
    }

    public sealed interface ProjectState {
        Stamp stamp();

        record Synchronized(Stamp stamp) implements ProjectState {
        }

        record Dirty(Stamp stamp, ExternalSystemModificationType type) implements ProjectState {
        }

        record Modified(Stamp stamp, ExternalSystemModificationType type) implements ProjectState {
        }

        record Reverted(Stamp stamp) implements ProjectState {
        }

        record Broken(Stamp stamp) implements ProjectState {
        }
    }

    public static final class Stamp implements Comparable<Stamp> {
        public static final Stamp NONE = new Stamp(-1);

        private static final AtomicInteger ourCounter = new AtomicInteger(0);

        private final int myStamp;

        private Stamp(int stamp) {
            myStamp = stamp;
        }

        public static Stamp nextStamp() {
            return new Stamp(ourCounter.incrementAndGet());
        }

        @Override
        public int compareTo(Stamp other) {
            return Integer.compare(myStamp, other.myStamp);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Stamp that)) {
                return false;
            }
            return myStamp == that.myStamp;
        }

        @Override
        public int hashCode() {
            return myStamp;
        }

        @Override
        public String toString() {
            return "Stamp(" + myStamp + ")";
        }
    }
}
