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
package consulo.ai.impl.internal.acp;

import consulo.ai.AIProvider;
import consulo.ai.acp.AcpAIProviderTypeBase;
import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersListUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Any agent of the Agent Client Protocol, run by the command the account gives - for the agents no plugin of their
 * own covers. A popular agent rather has a plugin, which signs in through an account.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtensionImpl
@Singleton
public class AcpGenericAIProviderType extends AcpAIProviderTypeBase {
    public static final String ID = "acp";

    static final String COMMAND = "acp.command";
    static final String ARGUMENTS = "acp.arguments";
    /**
     * {@code NAME=value} pairs, one a line.
     */
    static final String ENVIRONMENT = "acp.environment";

    private final ProcessHandlerBuilderFactory myProcessHandlerBuilderFactory;

    @Inject
    public AcpGenericAIProviderType(ProcessHandlerBuilderFactory processHandlerBuilderFactory) {
        super(processHandlerBuilderFactory);
        myProcessHandlerBuilderFactory = processHandlerBuilderFactory;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.acpProviderName();
    }

    @Override
    public Image getIcon() {
        return AIIconGroup.toolwindowAiassistant();
    }

    @Override
    protected @Nullable GeneralCommandLine createCommandLine(AIProvider provider) {
        return createCommandLine(provider.getOptions());
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createAccountComponent(Map<String, String> options, Disposable uiDisposable) {
        return new AcpGenericAccountComponent(myProcessHandlerBuilderFactory, options, uiDisposable).getComponent();
    }

    /**
     * The command of the options - a name is looked up on the PATH - or null when there is none to run.
     */
    static @Nullable GeneralCommandLine createCommandLine(Map<String, String> options) {
        String command = options.getOrDefault(COMMAND, "").strip();
        if (command.isEmpty()) {
            return null;
        }
        Path executable = Path.of(command);
        if (!executable.isAbsolute() || !Files.isExecutable(executable)) {
            executable = Platform.current().findExecutableInPath(command);
        }
        if (executable == null) {
            return null;
        }
        return new GeneralCommandLine()
            .withExecutablePath(executable)
            .withParameters(ParametersListUtil.parse(options.getOrDefault(ARGUMENTS, "")))
            .withEnvironment(environment(options.getOrDefault(ENVIRONMENT, "")))
            .withWorkingDirectory(Platform.current().user().homePath());
    }

    private static Map<String, String> environment(String text) {
        Map<String, String> environment = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            int equals = line.indexOf('=');
            if (equals > 0) {
                environment.put(line.substring(0, equals).strip(), line.substring(equals + 1).strip());
            }
        }
        return environment;
    }
}
