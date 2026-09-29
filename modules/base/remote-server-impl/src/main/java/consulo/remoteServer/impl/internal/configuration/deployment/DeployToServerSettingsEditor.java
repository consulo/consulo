// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public abstract class DeployToServerSettingsEditor<S extends ServerConfiguration, D extends DeploymentConfiguration>
    extends SettingsEditor<DeployToServerRunConfiguration<S, D>> {

    private final DeploymentConfigurator<D, S> myDeploymentConfigurator;
    private final Project myProject;
    private final RemoteServerComboWithAutoDetect<S> myServerCombo;
    private final DockLayout myDeploymentSettingsComponent;
    private @Nullable SettingsEditor<D> myDeploymentSettingsEditor;
    private @Nullable DeploymentSource myLastSelectedSource;
    private @Nullable RemoteServer<S> myLastSelectedServer;
    private @Nullable D myDeploymentConfiguration;

    @RequiredUIAccess
    public DeployToServerSettingsEditor(ServerType<S> type, DeploymentConfigurator<D, S> deploymentConfigurator, Project project) {
        myDeploymentConfigurator = deploymentConfigurator;
        myProject = project;

        myServerCombo = new RemoteServerComboWithAutoDetect<>(type, project);
        Disposer.register(this, myServerCombo);
        myServerCombo.addChangeListener(this::updateDeploymentSettingsEditor);

        myDeploymentSettingsComponent = DockLayout.create();
    }

    protected abstract @Nullable DeploymentSource getSelectedSource();

    @RequiredUIAccess
    protected abstract void resetSelectedSourceFrom(DeployToServerRunConfiguration<S, D> configuration);

    @RequiredUIAccess
    protected final void updateDeploymentSettingsEditor() {
        RemoteServer<S> selectedServer = myServerCombo.getSelectedServer();

        DeploymentSource selectedSource = getSelectedSource();
        if (Comparing.equal(selectedSource, myLastSelectedSource) && Comparing.equal(selectedServer, myLastSelectedServer)) {
            return;
        }

        if (!Comparing.equal(selectedSource, myLastSelectedSource)) {
            updateBeforeRunOptions(myLastSelectedSource, false);
            updateBeforeRunOptions(selectedSource, true);
        }
        if (selectedSource != null && !Disposer.isDisposed(this)) {
            SettingsEditor<D> deploymentSettingsEditor = myDeploymentConfigurator.createEditor(selectedSource, selectedServer);
            myDeploymentSettingsEditor = deploymentSettingsEditor;

            if (deploymentSettingsEditor != null) {
                Component component = deploymentSettingsEditor.getUIComponent();
                if (myDeploymentConfiguration != null) {
                    deploymentSettingsEditor.resetFrom(myDeploymentConfiguration);
                }

                deploymentSettingsEditor.addSettingsEditorListener(e -> fireEditorStateChanged());
                Disposer.register(this, deploymentSettingsEditor);

                myDeploymentSettingsComponent.center(component);
            }
        }
        myLastSelectedSource = selectedSource;
        myLastSelectedServer = selectedServer;
    }

    @SuppressWarnings("unchecked")
    private void updateBeforeRunOptions(@Nullable DeploymentSource source, boolean selected) {
        if (source != null) {
            DeploymentSourceType type = source.getType();
            type.updateBuildBeforeRunOption(myServerCombo.getComponent(), myProject, source, selected);
        }
    }

    @RequiredUIAccess
    @Override
    protected void resetEditorFrom(DeployToServerRunConfiguration<S, D> configuration) {
        myServerCombo.selectServerInCombo(configuration.getServerName());
        resetSelectedSourceFrom(configuration);

        D deploymentConfiguration = configuration.getDeploymentConfiguration();
        myDeploymentConfiguration = deploymentConfiguration;
        updateDeploymentSettingsEditor();
        if (deploymentConfiguration != null && myDeploymentSettingsEditor != null) {
            myDeploymentSettingsEditor.resetFrom(deploymentConfiguration);
        }
    }

    @RequiredUIAccess
    @Override
    protected void applyEditorTo(DeployToServerRunConfiguration<S, D> configuration) throws ConfigurationException {
        updateDeploymentSettingsEditor();

        myServerCombo.validateAutoDetectedItem();

        configuration.setServerName(Optional.ofNullable(myServerCombo.getSelectedServer()).map(RemoteServer::getName).orElse(null));
        DeploymentSource deploymentSource = getSelectedSource();
        configuration.setDeploymentSource(deploymentSource);

        if (deploymentSource != null) {
            D deployment = configuration.getDeploymentConfiguration();
            if (deployment == null) {
                deployment = myDeploymentConfigurator.createDefaultConfiguration(deploymentSource);
                configuration.setDeploymentConfiguration(deployment);
            }
            myDeploymentConfiguration = deployment;
            if (myDeploymentSettingsEditor != null) {
                myDeploymentSettingsEditor.applyTo(deployment);
            }
        }
        else {
            configuration.setDeploymentConfiguration(null);
        }
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(RemoteServerLocalize.labelTextServer(), myServerCombo.getComponent());

        addDeploymentSourceUi(builder);

        DockLayout panel = DockLayout.create();
        panel.top(builder.build());
        panel.center(myDeploymentSettingsComponent);
        return panel;
    }

    @RequiredUIAccess
    protected abstract void addDeploymentSourceUi(FormBuilder formBuilder);

    public static class AnySource<S extends ServerConfiguration, D extends DeploymentConfiguration>
        extends DeployToServerSettingsEditor<S, D> {

        private final ComboBox<DeploymentSource> mySourceComboBox;

        @RequiredUIAccess
        public AnySource(ServerType<S> type, DeploymentConfigurator<D, S> deploymentConfigurator, Project project) {
            super(type, deploymentConfigurator, project);

            List<DeploymentSource> sources = new ArrayList<>(deploymentConfigurator.getAvailableDeploymentSources());
            sources.sort(Comparator.comparing(deploymentSource -> deploymentSource.getPresentableName().get(), String.CASE_INSENSITIVE_ORDER));

            mySourceComboBox = ComboBox.create(sources);
            mySourceComboBox.setRender((presentation, item) -> {
                DeploymentSource value = item.getValue();
                if (value == null) {
                    return;
                }
                presentation.withIcon(value.getIcon());
                presentation.append(value.getPresentableName());
            });
            mySourceComboBox.addValueListener(e -> updateDeploymentSettingsEditor());
        }

        @Override
        protected @Nullable DeploymentSource getSelectedSource() {
            return mySourceComboBox.getValue();
        }

        @RequiredUIAccess
        @Override
        protected void resetSelectedSourceFrom(DeployToServerRunConfiguration<S, D> configuration) {
            mySourceComboBox.setValue(configuration.getDeploymentSource(), false);
        }

        @RequiredUIAccess
        @Override
        protected void addDeploymentSourceUi(FormBuilder formBuilder) {
            formBuilder.addLabeled(RemoteServerLocalize.labelTextDeployment(), mySourceComboBox);
        }
    }

    public static class LockedSource<S extends ServerConfiguration, D extends DeploymentConfiguration>
        extends DeployToServerSettingsEditor<S, D> {

        private final DeploymentSource myLockedSource;

        @RequiredUIAccess
        public LockedSource(
            ServerType<S> type,
            DeploymentConfigurator<D, S> deploymentConfigurator,
            Project project,
            DeploymentSource lockedSource
        ) {
            super(type, deploymentConfigurator, project);
            myLockedSource = lockedSource;
        }

        @RequiredUIAccess
        @Override
        protected void addDeploymentSourceUi(FormBuilder formBuilder) {
        }

        @RequiredUIAccess
        @Override
        protected void resetSelectedSourceFrom(DeployToServerRunConfiguration<S, D> configuration) {
            assert configuration.getDeploymentSource() == myLockedSource;
        }

        @Override
        protected DeploymentSource getSelectedSource() {
            return myLockedSource;
        }
    }
}
