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
package consulo.sandboxPlugin.ui.tab;

import consulo.localize.LocalizeValue;
import consulo.ui.DialogCancelledException;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class UITesterTabUtil {
    private UITesterTabUtil() {
    }

    @RequiredUIAccess
    static void report(String what, @Nullable Object answer, @Nullable Throwable error) {
        Throwable cause = error instanceof CompletionException || error instanceof ExecutionException
            ? error.getCause()
            : error;

        if (cause instanceof DialogCancelledException) {
            MessageBoxes.okInfo(LocalizeValue.of(what + " -> cancelled")).showAsync();
        }
        else if (cause != null) {
            MessageBoxes.okError(LocalizeValue.of(what + " failed: " + cause.getMessage())).showAsync();
        }
        else {
            MessageBoxes.okInfo(LocalizeValue.of(what + " -> " + answer)).showAsync();
        }
    }
}
