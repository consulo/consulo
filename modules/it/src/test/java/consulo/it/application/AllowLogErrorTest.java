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

import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.LoggedError;
import consulo.it.internal.HeadlessLoggerFactory;
import consulo.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the logged error guard in both directions - it is only useful if it is known to fire, and a suite in
 * which nothing ever logs an error cannot tell a working check from a dead one.
 * <p>
 * Every test consumes the recorded errors, since {@link HeadlessApplicationExtension} otherwise fails the test
 * from whatever is left in the list.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AllowLogErrorTest {
    private static final String CATEGORY = "consulo.it.application.AllowLogErrorTest";

    /**
     * Resolved per call rather than into a static field: a static field is initialized when the class is loaded,
     * which happens before the extension installs {@link HeadlessLoggerFactory}, and would capture the default
     * factory instead - the one which throws on {@code error(...)}.
     */
    private static Logger log() {
        return Logger.getInstance(AllowLogErrorTest.class);
    }

    @Test
    public void loggedErrorIsRecordedByDefault() {
        log().error("recorded by the guard under test");

        List<LoggedError> errors = HeadlessLoggerFactory.takeLoggedErrors();

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getCategory()).isEqualTo(CATEGORY);
        assertThat(errors.get(0).getMessage()).contains("recorded by the guard under test");
    }

    /**
     * The call must return so that the caller keeps running exactly as it does in production - a guard which
     * reports and continues is the whole point of recording rather than throwing.
     */
    @Test
    public void loggingAnErrorDoesNotThrow() {
        boolean reachedNextStatement = false;

        log().error("must not interrupt the caller");
        reachedNextStatement = true;

        assertThat(reachedNextStatement).as("Logger.error must return to its caller").isTrue();
        assertThat(HeadlessLoggerFactory.takeLoggedErrors()).hasSize(1);
    }

    @Test
    @AllowLogError
    public void loggedErrorIsIgnoredWhenAnnotated() {
        log().error("tolerated by the annotation");

        assertThat(HeadlessLoggerFactory.takeLoggedErrors())
            .as("an unfiltered annotation must tolerate every category")
            .isEmpty();
    }

    @Test
    @AllowLogError(CATEGORY)
    public void listedCategoryIsIgnoredWhenAnnotated() {
        log().error("tolerated by the listed category");

        assertThat(HeadlessLoggerFactory.takeLoggedErrors()).isEmpty();
    }

    /**
     * A narrowed opt-in must stay narrow, otherwise it silently covers defects the test was never meant to allow.
     */
    @Test
    @AllowLogError("consulo.it.some.other.category")
    public void categoryOutsideTheListIsStillRecorded() {
        log().error("outside the tolerated category");

        assertThat(HeadlessLoggerFactory.takeLoggedErrors()).hasSize(1);
    }
}
