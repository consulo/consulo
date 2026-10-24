// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.externalProcessAuthHelper;

import consulo.container.boot.ContainerPathManager;
import consulo.util.io.ClassPathUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class ScriptGeneratorUtil {
    private ScriptGeneratorUtil() {
    }

    private static File createBatchScript(String fileName, String commandLine) throws IOException {
        String batchScriptText = "@echo off" + "\r\n" + commandLine + " %*\r\n";
        return createTempExecutable(fileName + ".bat", batchScriptText);
    }

    private static File createShellScript(String fileName, String commandLine) throws IOException {
        String shellText = "#!/bin/sh\n" + commandLine + " \"$@\"\n";
        return createTempExecutable(fileName + ".sh", shellText);
    }

    private static File createTempExecutable(String fileName, String content) throws IOException {
        File file = new File(ContainerPathManager.get().getTempPath(), fileName);
        FileUtil.writeToFile(file, content);
        NioFiles.setExecutable(file.toPath());
        return file;
    }

    public static File createTempScript(String commandLine, String fileNamePrefix, boolean useBatchFile) throws IOException {
        return useBatchFile ? createBatchScript(fileNamePrefix, commandLine) : createShellScript(fileNamePrefix, commandLine);
    }

    /**
     * @return jar or directory that contains the class for the classpath
     */
    public static File getJarFileFor(Class<?> clazz) {
        return new File(Objects.requireNonNull(ClassPathUtil.getJarPathForClass(clazz)));
    }
}
