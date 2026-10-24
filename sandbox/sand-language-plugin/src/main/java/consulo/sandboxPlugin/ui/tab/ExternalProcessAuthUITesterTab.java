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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalProcessAuthHelper.AuthenticationMode;
import consulo.externalProcessAuthHelper.NativeSshAuthService;
import consulo.externalProcessAuthHelper.NativeSshGuiAuthenticator;
import consulo.externalProcessAuthHelper.RestrictingAuthenticationGate;
import consulo.externalProcessAuthHelper.ScriptGeneratorImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import externalApp.nativessh.NativeSshAskPassAppHandler;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.UUID;

@ExtensionImpl(id = "externalProcessAuth", order = "after account")
public class ExternalProcessAuthUITesterTab implements UITesterTab {
    private static final String PASSPHRASE_PROMPT = "Enter passphrase for key '/tmp/sand-test-key': ";
    private static final String PASSWORD_PROMPT = "sand@localhost's password: ";
    private static final String CONFIRM_PROMPT = "The authenticity of host 'localhost' can't be established.\n"
        + "Are you sure you want to continue connecting (yes/no/[fingerprint])? ";

    private final ProjectManager myProjectManager;
    private final NativeSshAuthService myNativeSshAuthService;

    @Inject
    public ExternalProcessAuthUITesterTab(ProjectManager projectManager, NativeSshAuthService nativeSshAuthService) {
        myProjectManager = projectManager;
        myNativeSshAuthService = nativeSshAuthService;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("External Process Auth");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        ComboBox<AuthenticationMode> mode = ComboBox.create(AuthenticationMode.values());
        mode.setValue(AuthenticationMode.FULL);
        layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Authentication mode"), mode));

        TextBox prompt = TextBox.create(PASSPHRASE_PROMPT);
        layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Prompt"), prompt));

        Label result = Label.create(LocalizeValue.localizeTODO("Run the askpass script to see what it prints"));

        layout.add(Button.create(LocalizeValue.localizeTODO("Passphrase prompt"), event -> prompt.setValue(PASSPHRASE_PROMPT)));
        layout.add(Button.create(LocalizeValue.localizeTODO("Password prompt"), event -> prompt.setValue(PASSWORD_PROMPT)));
        layout.add(Button.create(LocalizeValue.localizeTODO("Confirmation prompt"), event -> prompt.setValue(CONFIRM_PROMPT)));
        layout.add(Button.create(LocalizeValue.localizeTODO("Run askpass script"), event -> {
            Project project = findProject();
            if (project == null) {
                result.setText(LocalizeValue.localizeTODO("Open a project first - the prompts are shown in its window"));
                return;
            }
            AuthenticationMode authenticationMode = mode.getValueOrError();
            String description = prompt.getValueOrError();
            UIAccess uiAccess = UIAccess.current();
            result.setText(LocalizeValue.localizeTODO("Running..."));
            AppExecutorUtil.getAppExecutorService().execute(() -> {
                String text = runAskPass(project, authenticationMode, description);
                uiAccess.give(() -> result.setText(LocalizeValue.of(text)));
            });
        }));
        layout.add(result);
        return layout;
    }

    private @Nullable Project findProject() {
        Project[] projects = myProjectManager.getOpenProjects();
        return projects.length == 0 ? null : projects[0];
    }

    private String runAskPass(Project project, AuthenticationMode authenticationMode, String description) {
        Disposable disposable = Disposable.newDisposable("ExternalProcessAuthUITesterTab");
        try {
            NativeSshGuiAuthenticator authenticator =
                new NativeSshGuiAuthenticator(project, new RestrictingAuthenticationGate(), authenticationMode, false);
            UUID handlerId = myNativeSshAuthService.registerHandler(authenticator, disposable);
            File script = myNativeSshAuthService.getCallbackScriptPath("sand", new ScriptGeneratorImpl(), Platform.current().os().isWindows());

            GeneralCommandLine commandLine = new GeneralCommandLine(script.getPath(), description)
                .withEnvironment(NativeSshAskPassAppHandler.IJ_SSH_ASK_PASS_HANDLER_ENV, handlerId.toString())
                .withEnvironment(NativeSshAskPassAppHandler.IJ_SSH_ASK_PASS_PORT_ENV, Integer.toString(myNativeSshAuthService.getIdePort()));
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine);
            return "exit code: " + output.getExitCode()
                + "\nstdout: " + output.getStdout().trim()
                + "\nstderr: " + output.getStderr().trim()
                + "\nscript: " + script.getPath();
        }
        catch (Exception e) {
            return "failed: " + e;
        }
        finally {
            Disposer.dispose(disposable);
        }
    }
}
