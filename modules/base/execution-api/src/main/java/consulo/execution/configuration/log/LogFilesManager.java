/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution.configuration.log;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.process.ProcessHandler;
import consulo.util.lang.function.Conditions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

public class LogFilesManager {
    private final LogConsoleManager myManager;

    public LogFilesManager(@Nonnull LogConsoleManager manager) {
        myManager = manager;
    }

    public void addLogConsoles(@Nonnull RunConfigurationBase runConfiguration, @Nullable ProcessHandler startedProcess) {
        for (LogFileOptions logFileOptions : runConfiguration.getAllLogFiles()) {
            if (logFileOptions.isEnabled()) {
                addConfigurationConsoles(logFileOptions, Conditions.<String>alwaysTrue(), logFileOptions.getPaths(), runConfiguration);
            }
        }
        runConfiguration.createAdditionalTabComponents(myManager, startedProcess);
    }

    private void addConfigurationConsoles(
        @Nonnull LogFileOptions logFile,
        @Nonnull Predicate<String> shouldInclude,
        @Nonnull Set<String> paths,
        @Nonnull RunConfigurationBase runConfiguration
    ) {
        if (paths.isEmpty()) {
            return;
        }

        TreeMap<String, String> titleToPath = new TreeMap<>();
        if (paths.size() == 1) {
            String path = paths.iterator().next();
            if (shouldInclude.test(path)) {
                titleToPath.put(logFile.getName(), path);
            }
        }
        else {
            for (String path : paths) {
                if (shouldInclude.test(path)) {
                    String title = new File(path).getName();
                    if (titleToPath.containsKey(title)) {
                        title = path;
                    }
                    titleToPath.put(title, path);
                }
            }
        }

        for (String title : titleToPath.keySet()) {
            String path = titleToPath.get(title);
            assert path != null;
            myManager.addLogConsole(
                title,
                path,
                logFile.getCharset(),
                logFile.isSkipContent() ? new File(path).length() : 0,
                runConfiguration
            );
        }
    }
}
