/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.internal;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.util.collection.FList;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.function.ThrowableRunnable;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A utility to enforce "slow operation on EDT" assertion.
 * <p/>
 * That assertion is a tool we use now to split IntelliJ Platform into "frontend" and "backend" parts
 * to avoid UI freezes caused by inherently slow operations like indexes access, slow I/O, etc.
 *
 * @see #assertSlowOperationsAreAllowed()
 */
public final class SlowOperations {
    private static final class Holder {
        private static final Logger LOG = Logger.getInstance(SlowOperations.class);
    }

    /**
     * Set this property to force slow ops check
     */
    public static final String FORBID_SLOW_OPS_PROPERTY = "consulo.forbid.slow.ops";

    private static final Set<String> ourKnownIssues = ConcurrentHashMap.newKeySet();

    private static final String ERROR_EDT =
        "Slow operations are prohibited on EDT. See SlowOperations.assertSlowOperationsAreAllowed javadoc.";
    private static final String ERROR_RA =
        "Non-cancelable slow operations are prohibited inside read action. See SlowOperations.assertNonCancelableSlowOperationsAreAllowed javadoc.";

    /** Do not use. For Action System only */
    public static final String ACTION_UPDATE = "action.update";
    /** Mark entry-points to user-triggered actions. The assertion is suppressed for now. */
    public static final String ACTION_PERFORM = "action.perform";
    /** For muting noisy problems with issue tracker tickets */
    private static final String KNOWN_ISSUE = "known-issues";
    /** @deprecated Do not use. It is a to-be-deleted no-op */
    @Deprecated
    public static final String GENERIC = "generic";

    /** Do not use. For Action System only. The assertion is thrown even if disabled */
    public static final String FORCE_ASSERT = "  force assert  ";
    /** Do not use. For Action System only. The assertion is turned into PCE */
    public static final String FORCE_THROW = "  force throw  ";
    /** Do not use. For Action System only. It resets the section stack in modal dialogs */
    public static final String RESET = "  reset  ";

    /** VM property, set to {@code true} if running in plugin development sandbox. */
    public static final String PLUGIN_SANDBOX_MODE = "consulo.plugin.in.sandbox.mode";

    private static int ourAlwaysAllow = -1;
    private static FList<String> ourStack = FList.emptyList();

    private static @Nullable String ourTargetClass;
    private static final Set<String> ourReportedClasses = new HashSet<>();
    // stands in for the throwable interning in the original: report each distinct stack trace once
    private static final Set<Integer> ourReportedTraces = ConcurrentHashMap.newKeySet();

    private SlowOperations() {
    }

    /**
     * If you get an exception from this method, then you need to move the computation to a background thread (BGT)
     * and to avoid blocking the UI thread (EDT).
     * <p/>
     * To temporarily mute the assertion, file a ticket and use {@link #knownIssue(String)}.
     * The assertion inside named sections is turned on/off separately via Registry keys
     * {@code ide.slow.operations.assertion.<sectionName>}.
     * <p/>
     * Action Subsystem<br><br>
     * <ul>
     * <li>
     * {@code AnAction#update}, {@code ActionGroup#getChildren}, and {@code ActionGroup#canBePerformed} should be either fast
     * or moved to a background thread by returning {@code ActionUpdateThread#BGT} in {@code AnAction#getActionUpdateThread}.
     * </li>
     * <li>
     * Use {@code UiDataProvider} and {@code DataSink#lazy} to move slow code to BGT.
     * That slow code is called only if an action requests it.
     * </li>
     * <li>
     * {@code AnAction#actionPerformed} shall be explicitly coded not to block the UI thread.
     * </li>
     * </ul>
     */
    public static void assertSlowOperationsAreAllowed() {
        if (!isEdt()) {
            return;
        }
        if (isAlwaysAllowed() || isSlowOperationAllowed()) {
            return;
        }
        if (isInSection(FORCE_THROW)) {
            throw new SlowOperationCanceledException();
        }
        logError(ERROR_EDT);
    }

    /**
     * I/O and native calls in addition to being slow operations must not be called inside read-action (RA)
     * as such RAs cannot be promptly canceled on an incoming write-action (WA).
     *
     * @see #assertSlowOperationsAreAllowed()
     */
    public static void assertNonCancelableSlowOperationsAreAllowed() {
        if (isAlwaysAllowed()) {
            return;
        }
        if (isEdt()) {
            if (isSlowOperationAllowed()) {
                return;
            }
            logError(ERROR_EDT);
        }
        else {
            Application application = ApplicationManager.getApplication();
            if (application != null && application.isReadAccessAllowed()) {
                logError(ERROR_RA);
            }
        }
    }

    private static boolean isEdt() {
        Application application = ApplicationManager.getApplication();
        return application != null && application.isDispatchThread();
    }

    private static boolean isSlowOperationAllowed() {
        if (isInSection(FORCE_ASSERT)) {
            return false;
        }
        if (!Registry.is("ide.slow.operations.assertion", true)) {
            return true;
        }
        Application application = ApplicationManager.getApplication();
        if (application != null
            && application.isWriteAccessAllowed()
            && !Registry.is("ide.slow.operations.assertion.write.action", false)) {
            return true;
        }
        if (ourStack.isEmpty() && !Registry.is("ide.slow.operations.assertion.other", false)) {
            return true;
        }
        for (String activity : ourStack) {
            if (RESET.equals(activity)) {
                break;
            }
            if (!Registry.is("ide.slow.operations.assertion." + activity, true)) {
                return true;
            }
        }
        return false;
    }

    private static void logError(String message) {
        if (!isAlreadyReported()) {
            Holder.LOG.error(new Throwable(message));
        }
    }

    private static boolean isAlreadyReported() {
        if (ourTargetClass != null && !ourReportedClasses.add(ourTargetClass)) {
            return true;
        }
        return !ourReportedTraces.add(Arrays.hashCode(new Throwable().getStackTrace()));
    }

    public static boolean isInSection(String sectionName) {
        for (String activity : ourStack) {
            if (RESET.equals(activity)) {
                break;
            }
            if (sectionName.equals(activity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAlwaysAllowed() {
        if (SystemProperties.getBooleanProperty(FORBID_SLOW_OPS_PROPERTY, false)) {
            return false;
        }
        if (ourAlwaysAllow == 1) {
            return true;
        }
        if (ourAlwaysAllow == 0) {
            return false;
        }
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return true;
        }

        boolean result = application.isUnitTestMode()
            || application.isCommandLine()
            || !application.isInternal() && !SystemProperties.getBooleanProperty(PLUGIN_SANDBOX_MODE, false);
        ourAlwaysAllow = result ? 1 : 0;
        return result;
    }

    /**
     * @deprecated Redesign the logic - move BGT-only activities to BGT.
     * Otherwise, file a ticket and use {@link #knownIssue(String)} if not possible.
     */
    @Deprecated
    public static <T, E extends Throwable> T allowSlowOperations(ThrowableComputable<T, E> computable) throws E {
        return computable.compute();
    }

    /**
     * @deprecated Redesign the logic - move BGT-only activities to BGT.
     * Otherwise, file a ticket and use {@link #knownIssue(String)} if not possible.
     */
    @Deprecated
    public static <E extends Throwable> void allowSlowOperations(ThrowableRunnable<E> runnable) throws E {
        try (AccessToken ignore = startSection(GENERIC)) {
            runnable.run();
        }
    }

    public static AccessToken knownIssue(String issueId) {
        if (isEdt()) {
            ourKnownIssues.add(issueId);
        }

        return startSection(KNOWN_ISSUE);
    }

    public static Set<String> reportKnownIssues() {
        Set<String> result = new HashSet<>(ourKnownIssues);
        ourKnownIssues.clear();
        return result.stream()
            .flatMap(codes -> Arrays.stream(codes.split(",")))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    public static AccessToken reportOnceIfViolatedFor(Object target) {
        if (!isEdt()) {
            return AccessToken.EMPTY_ACCESS_TOKEN;
        }
        @Nullable String prev = ourTargetClass;
        ourTargetClass = target.getClass().getName();
        return new AccessToken() {
            @Override
            public void finish() {
                ourTargetClass = prev;
            }
        };
    }

    /**
     * @param activityName see {@link #startSection(String)} javadoc
     * @deprecated Redesign the logic - move BGT-only activities to BGT.
     * Otherwise, file a ticket and use {@link #knownIssue(String)} if not possible.
     */
    @Deprecated
    public static AccessToken allowSlowOperations(String activityName) {
        return startSection(activityName);
    }

    /**
     * Starts a named logical section. Logical sections are a tool to tackle the frontend/backend splitting part-by-part,
     * and not to be overwhelmed by all that needs to be reworked all at once. Some sections have additional, hard-coded
     * semantics, like {@link #FORCE_ASSERT}, and {@link #RESET}.
     * <p/>
     * <b>This method is not for muting the assertion in places. It is intended for the common platform code.</b>
     *
     * @param sectionName reuse {@link #GENERIC} and other existing section names as much as possible.
     *                    <p/>
     *                    Use a new name <b>iff</b> you need a dedicated on/off switch for the assertion inside.
     *                    In that case, remember to add the corresponding {@code ide.slow.operations.assertion.<sectionName>} Registry key.
     * @see Registry
     */
    public static AccessToken startSection(String sectionName) {
        if (!isEdt()) {
            return AccessToken.EMPTY_ACCESS_TOKEN;
        }

        FList<String> prev = ourStack;
        ourStack = prev.prepend(sectionName);
        return new AccessToken() {
            @Override
            public void finish() {
                ourStack = prev;
            }
        };
    }

    public static boolean isMyMessage(@Nullable String error) {
        return ERROR_EDT.equals(error) || ERROR_RA.equals(error);
    }
}
