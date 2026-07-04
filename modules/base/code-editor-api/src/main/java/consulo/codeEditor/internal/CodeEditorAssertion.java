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
package consulo.codeEditor.internal;

import consulo.application.Application;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.ThreadIssueException;

/**
 * This is hack - in idea world we must not allow running Code Editor code from read lock (not ui thread)
 *
 * @author VISTALL
 * @since 2026-06-27
 */
public class CodeEditorAssertion {
    private static boolean DISABLE_ALLOWING_READ_UNDER_READ_LOCK = Boolean.getBoolean("consulo.disable.allowing.read.under.read.lock");

    @RequiredUIAccess
    public static void assertEditorThreading() {
        if (!DISABLE_ALLOWING_READ_UNDER_READ_LOCK && Application.get().isReadAccessAllowed()) {
            // hack read action check
            return;
        }

        if (!UIAccess.isUIThread()) {
            throw new ThreadIssueException("Call must be called inside UI thread");
        }
    }
}
