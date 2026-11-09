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
package consulo.continuousIntegration.impl.internal.run;

import consulo.continuousIntegration.CICapability;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIParameterDefinition;
import consulo.continuousIntegration.CIParameterValue;
import consulo.continuousIntegration.CIRunRequest;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.continuousIntegration.ui.CIParameterEditor;
import consulo.continuousIntegration.ui.CIParameterEditorProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asks for the parameters of a build, and for the branch when the server can build any ref. One row per parameter:
 * the editor comes from the first {@link CIParameterEditorProvider} accepting it, otherwise from
 * {@link CIStandardParameterEditors}.
 *
 * @since 2026-11-09
 */
public final class CIRunBuildDialogDescriptor extends DialogDescriptor {
    private final Project myProject;
    private final CIServer myServer;
    private final CIJob myJob;
    private final List<CIParameterDefinition> myDefinitions;
    private final Map<CIParameterDefinition, CIParameterEditor> myEditors = new LinkedHashMap<>();
    private @Nullable TextBox myRefBox;
    private @Nullable Component myPreferredFocused;
    private @Nullable CIRunRequest myRequest;

    private CIRunBuildDialogDescriptor(Project project, CIServer server, CIJob job, List<CIParameterDefinition> definitions) {
        super(ContinuousIntegrationImplLocalize.runDialogTitle(job.displayName()));
        myProject = project;
        myServer = server;
        myJob = job;
        myDefinitions = definitions;
    }

    /**
     * @return the request to start, empty when the dialog was cancelled
     */
    @RequiredUIAccess
    public static CompletableFuture<Optional<CIRunRequest>> showAsync(
        Project project,
        CIServer server,
        CIJob job,
        List<CIParameterDefinition> definitions
    ) {
        CIRunBuildDialogDescriptor descriptor = new CIRunBuildDialogDescriptor(project, server, job, definitions);
        Dialog dialog = project.getApplication().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync((value, error) -> {
            CIRunRequest request = descriptor.myRequest;
            return error != null || request == null ? Optional.<CIRunRequest>empty() : Optional.of(request);
        }, UIAccess.current());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create();
        FormBuilder form = FormBuilder.create();
        boolean formHasRows = false;

        if (myServer.hasCapability(CICapability.RUN_WITH_REF)) {
            TextBox refBox = TextBox.create();
            refBox.setPlaceholder(ContinuousIntegrationImplLocalize.runDialogRefPlaceholder());
            form.addLabeled(ContinuousIntegrationImplLocalize.runDialogRef(), refBox);
            formHasRows = true;
            myRefBox = refBox;
            myPreferredFocused = refBox;
        }

        for (CIParameterDefinition definition : myDefinitions) {
            CIParameterEditor editor = createEditor(definition, uiDisposable);
            myEditors.put(definition, editor);
            if (myPreferredFocused == null) {
                myPreferredFocused = editor.getComponent();
            }

            if (editor.isFullWidth()) {
                if (formHasRows) {
                    root.add(form.build());
                    form = FormBuilder.create();
                    formHasRows = false;
                }
                root.add(editor.getComponent());
                continue;
            }

            Label label = Label.create(LocalizeValue.join(LocalizeValue.of(definition.name()), LocalizeValue.colon()));
            String description = definition.description();
            if (!StringUtil.isEmptyOrSpaces(description)) {
                label.setToolTipText(LocalizeValue.of(description));
                editor.getComponent().setToolTipText(LocalizeValue.of(description));
            }
            form.addLabeled(label, editor.getComponent());
            formHasRows = true;
        }

        if (formHasRows) {
            root.add(form.build());
        }
        return ScrollableLayout.create(root);
    }

    @RequiredUIAccess
    private CIParameterEditor createEditor(CIParameterDefinition definition, Disposable uiDisposable) {
        CIParameterEditorProvider provider = myProject.getApplication()
            .getExtensionPoint(CIParameterEditorProvider.class)
            .findFirstSafe(candidate -> candidate.accepts(myServer, definition));
        if (provider != null) {
            return provider.create(myServer, myJob, definition, uiDisposable);
        }
        return CIStandardParameterEditors.create(myProject, definition, uiDisposable);
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myPreferredFocused;
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (!(action instanceof DialogOkAction)) {
            return true;
        }

        Map<String, CIParameterValue> parameters = new LinkedHashMap<>();
        for (Map.Entry<CIParameterDefinition, CIParameterEditor> entry : myEditors.entrySet()) {
            CIParameterDefinition definition = entry.getKey();
            CIParameterValue parameterValue = entry.getValue().getValue();
            if (definition.required() && isEmpty(parameterValue)) {
                MessageBoxes.okError(ContinuousIntegrationImplLocalize.runDialogRequired(definition.name()))
                    .showAsync(entry.getValue().getComponent());
                return false;
            }
            if (parameterValue != null) {
                parameters.put(definition.name(), parameterValue);
            }
        }

        TextBox refBox = myRefBox;
        String ref = refBox == null ? null : StringUtil.nullize(refBox.getValue(), true);
        myRequest = new CIRunRequest(ref == null ? null : ref.trim(), parameters);
        return true;
    }

    private static boolean isEmpty(@Nullable CIParameterValue value) {
        return value == null || value instanceof CIParameterValue.Text text && text.value().isEmpty();
    }
}
