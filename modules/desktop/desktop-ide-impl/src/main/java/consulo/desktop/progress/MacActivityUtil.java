/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.progress;

import com.sun.jna.Pointer;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
public class MacActivityUtil {
  @SuppressWarnings("unused")
  private static class NSActivityOptions {
    // Used for activities that require the computer to not idle sleep. This is included in NSActivityUserInitiated.
    private static final long idleSystemSleepDisabled = 1L << 20;

    // App is performing a user-requested action.
    private static final long userInitiated = 0x00FFFFFFL | idleSystemSleepDisabled;
    private static final long userInitiatedAllowingIdleSystemSleep = userInitiated & ~idleSystemSleepDisabled;

    // Used for activities that require the highest amount of timer and I/O precision available. Very few applications should need to use this constant.
    private static final long latencyCritical = 0xFF00000000L;
  }

  public interface Activity extends Runnable {
    /**
     * Ends activity, allowing macOS to trigger AppNap (idempotent).
     */
    void run();
  }

  private static final class ActivityImpl extends AtomicReference<ID> implements Activity {
    private static final ID processInfoCls = Foundation.getObjcClass("NSProcessInfo");
    private static final Pointer processInfoSel = Foundation.createSelector("processInfo");
    private static final Pointer beginActivityWithOptionsReasonSel = Foundation.createSelector("beginActivityWithOptions:reason:");
    private static final Pointer endActivitySel = Foundation.createSelector("endActivity:");
    private static final Pointer retainSel = Foundation.createSelector("retain");
    private static final Pointer releaseSel = Foundation.createSelector("release");

    private ActivityImpl(@Nonnull Object reason) {
      super(begin(reason));
    }

    @Override
    public void run() {
      end(getAndSet(null));
    }

    private static ID getProcessInfo() {
      return Foundation.invoke(processInfoCls, processInfoSel);
    }

    private static ID begin(@Nonnull Object reason) {
      // http://lists.apple.com/archives/java-dev/2014/Feb/msg00053.html
      // https://developer.apple.com/library/prerelease/ios/documentation/Cocoa/Reference/Foundation/Classes/NSProcessInfo_Class/index.html#//apple_ref/c/tdef/NSActivityOptions
      return Foundation.invoke(Foundation.invoke(getProcessInfo(),
                                                 beginActivityWithOptionsReasonSel,
                                                 NSActivityOptions.userInitiatedAllowingIdleSystemSleep,
                                                 Foundation.nsString(reason.toString())), retainSel);
    }

    private static void end(@Nullable ID activityToken) {
      if (activityToken == null) return;

      Foundation.invoke(getProcessInfo(), endActivitySel, activityToken);
      Foundation.invoke(activityToken, releaseSel);
    }
  }

  public static Activity wakeUpNeo(@Nonnull Object reason) {
    return new ActivityImpl(reason);
  }
}
