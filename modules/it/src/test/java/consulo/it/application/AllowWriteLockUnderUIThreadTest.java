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
package consulo.it.application;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.it.AllowWriteLockUnderUIThread;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessApplicationImpl;
import consulo.ui.UIAccess;
import consulo.util.concurrent.ThreadIssueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the write-lock-under-UI-thread guard in both directions - it is only useful if it is known to fire,
 * and a suite in which nothing ever violates it cannot tell a working check from a dead one.
 * <p>
 * Both tests consume the recorded issues, since {@link HeadlessApplicationExtension} otherwise fails the test
 * from whatever is left in the list.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AllowWriteLockUnderUIThreadTest {
    private static final long TIMEOUT_SECONDS = 60;

    /**
     * Swallowing the exception is deliberate: it is what the production callers of a write action do, and the
     * issue must survive that and still be recorded.
     */
    @Test
    public void writeLockUnderUIThreadIsRejectedByDefault(Application application) throws Exception {
        UIAccess uiAccess = application.getLastUIAccess();

        boolean rejected = uiAccess.<Boolean>giveAsync(() -> {
            try {
                WriteAction.run(() -> {
                });
                return false;
            }
            catch (ThreadIssueException e) {
                return true;
            }
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(rejected).as("write action from the UI thread must throw by default").isTrue();
        assertThat(HeadlessApplicationImpl.takeThreadIssues())
            .as("the issue must be recorded as well as thrown")
            .hasSize(1);
    }

    @Test
    @AllowWriteLockUnderUIThread
    public void writeLockUnderUIThreadIsAllowedWhenAnnotated(Application application) throws Exception {
        UIAccess uiAccess = application.getLastUIAccess();

        boolean written = uiAccess.<Boolean>giveAsync(() -> {
            WriteAction.run(() -> {
            });
            return true;
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(written).as("the annotated test must be able to take the write lock from the UI thread").isTrue();
        assertThat(HeadlessApplicationImpl.takeThreadIssues()).isEmpty();
    }

    /**
     * The outermost write action is the only one which acquires anything, so a nested one must pass through even
     * though it also runs on the UI thread.
     */
    @Test
    @AllowWriteLockUnderUIThread
    public void nestedWriteActionIsNotRejected(Application application) throws Exception {
        UIAccess uiAccess = application.getLastUIAccess();

        boolean nested = uiAccess.<Boolean>giveAsync(() -> WriteAction.compute(() -> {
            HeadlessApplicationImpl.setAllowWriteLockUnderUIThread(false);
            try {
                return WriteAction.compute(() -> Boolean.TRUE);
            }
            finally {
                HeadlessApplicationImpl.setAllowWriteLockUnderUIThread(true);
            }
        })).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(nested).as("a nested write action acquires nothing and must not be rejected").isTrue();
        assertThat(HeadlessApplicationImpl.takeThreadIssues()).isEmpty();
    }
}
