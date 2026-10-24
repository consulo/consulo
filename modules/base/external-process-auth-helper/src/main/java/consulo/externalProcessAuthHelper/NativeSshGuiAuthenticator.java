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

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialAttributesUtil;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.externalProcessAuthHelper.localize.ExternalProcessAuthHelperLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.InputBoxBuilder;
import consulo.ui.MessageBoxes;
import externalApp.nativessh.NativeSshAskPassAppHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Matcher;

public class NativeSshGuiAuthenticator implements NativeSshAskPassAppHandler {
    private static final Logger LOG = Logger.getInstance(NativeSshGuiAuthenticator.class);

    private final @Nullable Project myProject;
    private final AuthenticationGate myAuthenticationGate;
    private final AuthenticationMode myAuthenticationMode;
    private final boolean myDoNotRememberPasswords;

    private final List<PromptHandler> myPasswordHandlers = List.of(
        new KeyPassphrasePromptHandler(),
        new SshPasswordPromptHandler(),
        new SshPinPromptHandler(),
        new ConfirmationPromptHandler()
    );

    public NativeSshGuiAuthenticator(@Nullable Project project,
                                     AuthenticationGate authenticationGate,
                                     AuthenticationMode authenticationMode,
                                     boolean doNotRememberPasswords) {
        myProject = project;
        myAuthenticationGate = authenticationGate;
        myAuthenticationMode = authenticationMode;
        myDoNotRememberPasswords = doNotRememberPasswords;
    }

    @Override
    public @Nullable String handleInput(String description) {
        LOG.debug("handleInput: " + description + ", mode: " + myAuthenticationMode);

        if (myAuthenticationMode == AuthenticationMode.NONE) {
            LOG.debug("authenticationMode: NONE");
            return null;
        }
        return myAuthenticationGate.waitAndCompute(() -> doHandleInput(description));
    }

    private @Nullable String doHandleInput(String description) {
        for (PromptHandler passwordHandler : myPasswordHandlers) {
            PromptAnswer answer = passwordHandler.handleInput(description);
            if (answer instanceof PromptAnswer.Answer value) {
                LOG.debug("handling using: " + passwordHandler);
                return value.value();
            }
        }

        LOG.debug("handling using generic prompt");
        return askGenericInput(description);
    }

    private class KeyPassphrasePromptHandler extends PasswordPromptHandler {
        @Override
        LocalizeValue getTitle() {
            return ExternalProcessAuthHelperLocalize.sshAskPassphraseTitle();
        }

        @Override
        String getServiceName() {
            return ExternalProcessAuthHelperLocalize.labelCredentialStoreKeySshPassphrase().get();
        }

        @Override
        @Nullable Prompt parseDescription(String description) {
            Matcher matcher = SshPrompts.PASSPHRASE_PROMPT.matcher(description);
            if (!matcher.matches()) {
                return null;
            }

            String keyPath = SshPrompts.extractKeyPath(matcher);
            return new Prompt(keyPath, ExternalProcessAuthHelperLocalize.sshAskPassphraseMessage(keyPath));
        }
    }

    private class SshPasswordPromptHandler extends PasswordPromptHandler {
        @Override
        LocalizeValue getTitle() {
            return ExternalProcessAuthHelperLocalize.sshPasswordTitle();
        }

        @Override
        String getServiceName() {
            return ExternalProcessAuthHelperLocalize.labelCredentialStoreKeySshPassword().get();
        }

        @Override
        @Nullable Prompt parseDescription(String description) {
            Matcher matcher = SshPrompts.PASSWORD_PROMPT.matcher(description);
            if (!matcher.matches()) {
                return null;
            }

            String username = SshPrompts.extractUsername(matcher);
            return new Prompt(username, ExternalProcessAuthHelperLocalize.sshPasswordMessage(username));
        }
    }

    private class SshPinPromptHandler extends PasswordPromptHandler {
        @Override
        LocalizeValue getTitle() {
            return ExternalProcessAuthHelperLocalize.sshAskPinTitle();
        }

        @Override
        String getServiceName() {
            return ExternalProcessAuthHelperLocalize.labelCredentialStoreKeySshPin().get();
        }

        @Override
        @Nullable Prompt parseDescription(String description) {
            Matcher matcher = SshPrompts.PKCS_PIN_TOKEN_PROMPT.matcher(description);
            if (!matcher.matches()) {
                return null;
            }

            String username = SshPrompts.extractPkcsTokenLabel(matcher);
            return new Prompt(username, ExternalProcessAuthHelperLocalize.sshAskPinMessage(username));
        }
    }

    private abstract class PasswordPromptHandler implements PromptHandler {
        private @Nullable String myLastAskedKey;

        abstract LocalizeValue getTitle();

        abstract String getServiceName();

        abstract @Nullable Prompt parseDescription(String description);

        @Override
        public PromptAnswer handleInput(String description) {
            Prompt prompt = parseDescription(description);
            if (prompt == null) {
                return PromptAnswer.NotHandled.INSTANCE;
            }

            boolean resetPassword = prompt.askedKey().equals(myLastAskedKey);
            myLastAskedKey = prompt.askedKey();

            return new PromptAnswer.Answer(askPassword(prompt, resetPassword));
        }

        private @Nullable String askPassword(Prompt prompt, boolean resetPassword) {
            if (myAuthenticationMode == AuthenticationMode.NONE) {
                return null;
            }

            if (myDoNotRememberPasswords) {
                return askUserOnEdt(() -> InputBoxBuilder.password().title(getTitle()).text(prompt.promptMessage()).showAsync(myProject));
            }

            String serviceName = CredentialAttributesUtil.generateServiceName(getServiceName(), prompt.askedKey());
            CredentialAttributes credentialAttributes = new CredentialAttributes(serviceName, prompt.askedKey());

            if (!resetPassword) {
                Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
                if (credentials != null) {
                    String password = credentials.getPasswordAsString();
                    if (password != null) {
                        return password;
                    }
                }
            }

            if (myAuthenticationMode == AuthenticationMode.SILENT) {
                return null;
            }

            String password = askUserOnEdt(() -> InputBoxBuilder.password().title(getTitle()).text(prompt.promptMessage()).showAsync(myProject));
            if (password != null) {
                PasswordSafe.getInstance().set(credentialAttributes, new Credentials(prompt.askedKey(), password));
            }
            return password;
        }
    }

    private class ConfirmationPromptHandler implements PromptHandler {
        private @Nullable String myLastAskedConfirmationInput;

        @Override
        public PromptAnswer handleInput(String description) {
            if (!description.contains(SshPrompts.CONFIRM_CONNECTION_PROMPT)) {
                return PromptAnswer.NotHandled.INSTANCE;
            }

            String message = SshPrompts.stripConfirmConnectionOptions(description);

            String knownAnswer = myAuthenticationGate.getSavedInput(message);
            if (knownAnswer != null && myLastAskedConfirmationInput == null) {
                myLastAskedConfirmationInput = knownAnswer;
                return new PromptAnswer.Answer(knownAnswer);
            }

            String answer = askUserOnEdt(() -> MessageBoxes.yesNo()
                .title(ExternalProcessAuthHelperLocalize.titleSshConfirmation())
                .text(LocalizeValue.of(message))
                .showAsync(myProject)
                .thenApply(yes -> Boolean.TRUE.equals(yes) ? "yes" : "no"));
            if (answer != null) {
                myAuthenticationGate.saveInput(message, answer);
            }
            return new PromptAnswer.Answer(answer);
        }
    }

    private @Nullable String askGenericInput(String description) {
        return askUserOnEdt(() -> InputBoxBuilder.password()
            .title(ExternalProcessAuthHelperLocalize.sshKeyboardInteractiveTitle())
            .text(LocalizeValue.of(description))
            .showAsync(myProject));
    }

    private @Nullable String askUserOnEdt(Supplier<CompletableFuture<@Nullable String>> query) {
        Project project = myProject;
        if (myAuthenticationMode != AuthenticationMode.FULL || project == null) {
            return null;
        }
        try {
            return project.getUIAccess().giveAsync(query).thenCompose(future -> future).get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (ExecutionException e) {
            LOG.warn(e);
            return null;
        }
    }

    private interface PromptHandler {
        PromptAnswer handleInput(String description);
    }

    private sealed interface PromptAnswer permits PromptAnswer.NotHandled, PromptAnswer.Answer {
        enum NotHandled implements PromptAnswer {
            INSTANCE
        }

        record Answer(@Nullable String value) implements PromptAnswer {
        }
    }

    private record Prompt(String askedKey, LocalizeValue promptMessage) {
    }
}
