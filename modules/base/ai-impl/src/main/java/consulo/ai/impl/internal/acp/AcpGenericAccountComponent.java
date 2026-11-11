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

import consulo.ai.acp.AcpAgent;
import consulo.ai.localize.AILocalize;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * The command of a generic agent, and a check that it speaks the protocol - which also offers the ways the agent signs
 * in. The agent keeps its login itself.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
final class AcpGenericAccountComponent {
    private final ProcessHandlerBuilderFactory myProcessHandlerBuilderFactory;
    private final Map<String, String> myOptions;
    private final UIAccess myUIAccess;

    private final VerticalLayout myRoot = VerticalLayout.create();
    private final Label myStatus = Label.create(LocalizeValue.empty());
    private final HorizontalLayout mySignIn = HorizontalLayout.create();
    private final Button myConnect;

    private @Nullable AcpAgent myAgent;
    private boolean myDisposed;

    @RequiredUIAccess
    AcpGenericAccountComponent(ProcessHandlerBuilderFactory processHandlerBuilderFactory, Map<String, String> options, Disposable uiDisposable) {
        myProcessHandlerBuilderFactory = processHandlerBuilderFactory;
        myOptions = options;
        myUIAccess = UIAccess.current();

        myRoot.add(field(AILocalize.acpLabelCommand(), AcpGenericAIProviderType.COMMAND, AILocalize.acpPlaceholderCommand()));
        myRoot.add(field(AILocalize.acpLabelArguments(), AcpGenericAIProviderType.ARGUMENTS, AILocalize.acpPlaceholderArguments()));
        myRoot.add(field(AILocalize.acpLabelEnvironment(), AcpGenericAIProviderType.ENVIRONMENT, AILocalize.acpPlaceholderEnvironment()));

        myConnect = Button.create(AILocalize.acpButtonCheck(), event -> check());
        myRoot.add(DockLayout.create().left(myStatus).right(myConnect));
        myRoot.add(mySignIn);

        Disposer.register(uiDisposable, () -> {
            myDisposed = true;
            stop();
        });
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private Component field(LocalizeValue label, String option, LocalizeValue placeholder) {
        TextBox box = TextBox.create(myOptions.getOrDefault(option, ""));
        box.setPlaceholder(placeholder);
        box.addValueListener(event -> {
            String value = event.getValue();
            if (value == null || value.isBlank()) {
                myOptions.remove(option);
            }
            else {
                myOptions.put(option, value);
            }
        });
        return DockLayout.create().left(Label.create(label)).center(box);
    }

    /**
     * Starts the agent of the command as written now, and says what it is and how it signs in.
     */
    @RequiredUIAccess
    private void check() {
        stop();
        mySignIn.removeAll();
        GeneralCommandLine commandLine = AcpGenericAIProviderType.createCommandLine(myOptions);
        if (commandLine == null) {
            myStatus.setText(AILocalize.acpStatusNoCommand());
            return;
        }

        myConnect.setEnabled(false);
        myStatus.setText(AILocalize.acpStatusStarting());
        AcpAgent.start(myProcessHandlerBuilderFactory, commandLine).whenCompleteAsync((agent, error) -> {
            myConnect.setEnabled(true);
            if (myDisposed) {
                if (agent != null) {
                    agent.close();
                }
                return;
            }
            if (agent == null) {
                myStatus.setText(AILocalize.acpStatusFailed(message(error)));
                return;
            }
            myAgent = agent;
            String name = agent.getAgentName();
            String version = agent.getAgentVersion();
            myStatus.setText(AILocalize.acpStatusConnected(
                name == null ? commandLine.getExePath() : name,
                version == null ? "" : version
            ));
            for (AcpAgent.AuthMethod method : agent.getAuthMethods()) {
                Button button = Button.create(AILocalize.acpButtonSignIn(method.name()), event -> signIn(agent, method));
                if (method.description() != null) {
                    button.setToolTipText(LocalizeValue.of(method.description()));
                }
                mySignIn.add(button);
            }
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void signIn(AcpAgent agent, AcpAgent.AuthMethod method) {
        myStatus.setText(AILocalize.acpStatusSigningIn(method.name()));
        agent.authenticate(method.id()).whenCompleteAsync((result, error) -> {
            if (myDisposed) {
                return;
            }
            myStatus.setText(error == null ? AILocalize.acpStatusSignedIn(method.name()) : AILocalize.acpStatusFailed(message(error)));
        }, myUIAccess);
    }

    private void stop() {
        AcpAgent agent = myAgent;
        myAgent = null;
        if (agent != null) {
            agent.close();
        }
    }

    private static String message(@Nullable Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause == null ? "" : String.valueOf(cause.getMessage());
    }
}
