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
package consulo.execution.ui.console;

import consulo.execution.internal.ConsoleHistoryControllerInternal;
import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2026-01-31
 */
public interface ConsoleHistoryController {
    public static ConsoleHistoryController getController(LanguageConsoleView console) {
        return console.getVirtualFile().getUserData(ConsoleHistoryControllerInternal.CONTROLLER_KEY);
    }

    public static void addToHistory(@Nonnull LanguageConsoleView consoleView, @Nullable String command) {
        ConsoleHistoryController controller = getController(consoleView);
        if (controller != null) {
            controller.addToHistory(command);
        }
    }

    void addToHistory(@Nullable String command);

    AnAction getBrowseHistory();
}
