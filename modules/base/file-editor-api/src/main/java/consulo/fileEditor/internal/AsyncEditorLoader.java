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
package consulo.fileEditor.internal;

import consulo.codeEditor.Editor;
import consulo.ui.UIAccess;
import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 2026-02-28
 */
public interface AsyncEditorLoader {
    Key<AsyncEditorLoader> ASYNC_LOADER = Key.create("ASYNC_LOADER");

    public static void performWhenLoaded(Editor editor, Runnable runnable) {
        UIAccess.assertIsUIThread();
        AsyncEditorLoader loader = editor.getUserData(ASYNC_LOADER);
        if (loader == null) {
            runnable.run();
        }
        else {
            loader.addDelayedAction(runnable);
        }
    }

    public static boolean isEditorLoaded(Editor editor) {
        return editor.getUserData(ASYNC_LOADER) == null;
    }

    void addDelayedAction(Runnable runnable);
}
