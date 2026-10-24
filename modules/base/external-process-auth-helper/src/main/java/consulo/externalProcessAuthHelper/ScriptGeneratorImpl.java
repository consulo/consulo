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

import consulo.platform.Platform;
import consulo.util.lang.StringUtil;
import externalApp.ExternalApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ScriptGeneratorImpl implements ScriptGenerator {
    private final List<String> myInternalParameters = new ArrayList<>();

    /**
     * Add internal parameters for the script
     */
    public ScriptGeneratorImpl addParameters(String... parameters) {
        Collections.addAll(myInternalParameters, parameters);
        return this;
    }

    @Override
    public String commandLine(Class<? extends ExternalApp> mainClass, boolean useBatchFile) {
        Set<File> jarPaths = new LinkedHashSet<>();
        addClasses(jarPaths, mainClass);
        addClasses(jarPaths, ExternalApp.class);

        StringBuilder cmd = new StringBuilder();

        cmd.append('"');
        cmd.append(getJavaExecutablePath());
        cmd.append('"');

        cmd.append(" -cp ");
        cmd.append('"');
        String classpathSeparator = String.valueOf(File.pathSeparatorChar);
        cmd.append(StringUtil.join(jarPaths, File::getPath, classpathSeparator));
        cmd.append('"');

        cmd.append(' ');
        cmd.append(mainClass.getName());

        for (String p : myInternalParameters) {
            cmd.append(' ');
            cmd.append(p);
        }

        String line = cmd.toString();
        if (Platform.current().os().isWindows()) {
            line = line.replace('\\', '/');
        }
        return line;
    }

    protected String getJavaExecutablePath() {
        return String.format("%s/bin/java", System.getProperty("java.home"));
    }

    private static void addClasses(Set<? super File> paths, Class<?> clazz) {
        paths.add(ScriptGeneratorUtil.getJarFileFor(clazz));
    }
}
