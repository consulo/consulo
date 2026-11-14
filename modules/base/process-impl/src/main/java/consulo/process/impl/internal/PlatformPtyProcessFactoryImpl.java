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
package consulo.process.impl.internal;

import com.pty4j.PtyProcessBuilder;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.impl.internal.local.LocalPlatformPtyProcess;
import consulo.process.internal.PlatformProcessHandlerFactory;
import consulo.process.internal.PlatformPtyProcess;
import consulo.process.internal.PlatformPtyProcessFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pseudo terminal of the machine of the command line - of its platform, or of the IDE machine through pty4j.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceImpl
@Singleton
public class PlatformPtyProcessFactoryImpl implements PlatformPtyProcessFactory {
    private final Application myApplication;

    @Inject
    public PlatformPtyProcessFactoryImpl(Application application) {
        myApplication = application;
    }

    @Override
    public PlatformPtyProcess createPtyProcess(GeneralCommandLine commandLine, int columns, int rows) throws ExecutionException {
        Platform platform = commandLine.getPlatform();
        PlatformProcessHandlerFactory platformFactory =
            myApplication.getExtensionPoint(PlatformProcessHandlerFactory.class).findFirstSafe(factory -> factory.isApplicable(platform));
        if (platformFactory != null) {
            return platformFactory.createPtyProcess(commandLine, columns, rows);
        }

        Path executable = commandLine.getExecutable();
        if (executable == null) {
            throw new ExecutionException("Executable is not specified");
        }
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(commandLine.getParametersList().getList());

        // a pseudo terminal takes the whole environment
        Map<String, String> environment = new HashMap<>(Platform.current().os().environmentVariables());
        environment.putAll(commandLine.getEnvironment());

        Path workingDirectory = commandLine.getWorkingDirectory();
        PtyProcessBuilder builder = new PtyProcessBuilder(command.toArray(String[]::new))
            .setEnvironment(environment)
            .setInitialColumns(columns)
            .setInitialRows(rows);
        if (workingDirectory != null) {
            builder.setDirectory(workingDirectory.toString());
        }
        try {
            return new LocalPlatformPtyProcess(builder.start());
        }
        catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }
}
