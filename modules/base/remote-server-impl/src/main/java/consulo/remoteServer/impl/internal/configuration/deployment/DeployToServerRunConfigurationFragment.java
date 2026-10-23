package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
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

/**
 * Server, deployment source and the deployment settings of a {@link DeployToServerRunConfiguration}; the settings rows come from
 * the {@link DeploymentConfigurator} and are rebuilt when the source or the server changes.
 */
final class DeployToServerRunConfigurationFragment<S extends ServerConfiguration, D extends DeploymentConfiguration>
    implements RunConfigurationFragment<RunConfiguration> {

    private final ServerType<S> myServerType;
    private final DeploymentConfigurator<D, S> myDeploymentConfigurator;
    private final @Nullable DeploymentSource myLockedSource;

    DeployToServerRunConfigurationFragment(ServerType<S> serverType,
                                           DeploymentConfigurator<D, S> deploymentConfigurator,
                                           @Nullable DeploymentSource lockedSource) {
        myServerType = serverType;
        myDeploymentConfigurator = deploymentConfigurator;
        myLockedSource = lockedSource;
    }

    @Override
    public String getId() {
        return "deployment";
    }

    @Override
    public LocalizeValue getName() {
        return RemoteServerLocalize.deploytoserverrunconfigurationTabTitleDeployment();
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        return new Editor(project, parent);
    }

    private final class Editor implements RunConfigurationFragmentEditor<RunConfiguration> {
        private final Project myProject;
        private final Disposable myParent;
        private final RemoteServerComboWithAutoDetect<S> myServerCombo;
        private final @Nullable ComboBox<DeploymentSource> mySourceComboBox;
        private final DockLayout myDeploymentSettingsComponent = DockLayout.create();
        private @Nullable RunConfigurationFragmentEditor<D> myDeploymentSettingsEditor;
        private @Nullable DeploymentSource myLastSelectedSource;
        private @Nullable RemoteServer<S> myLastSelectedServer;
        private @Nullable D myDeploymentConfiguration;

        @RequiredUIAccess
        private Editor(Project project, Disposable parent) {
            myProject = project;
            myParent = parent;

            myServerCombo = new RemoteServerComboWithAutoDetect<>(myServerType, project);
            Disposer.register(parent, myServerCombo);
            myServerCombo.addChangeListener(this::updateDeploymentSettingsEditor);

            if (myLockedSource == null) {
                List<DeploymentSource> sources = new ArrayList<>(myDeploymentConfigurator.getAvailableDeploymentSources());
                sources.sort(Comparator.comparing(source -> source.getPresentableName().get(), String.CASE_INSENSITIVE_ORDER));

                ComboBox<DeploymentSource> sourceComboBox = ComboBox.create(sources);
                sourceComboBox.setRenderer((presentation, item) -> {
                    DeploymentSource value = item.getValue();
                    if (value != null) {
                        presentation.withIcon(value.getIcon());
                        presentation.append(value.getPresentableName());
                    }
                });
                sourceComboBox.addValueListener(e -> updateDeploymentSettingsEditor());
                mySourceComboBox = sourceComboBox;
            }
            else {
                mySourceComboBox = null;
            }
        }

        @RequiredUIAccess
        @Override
        public void addRows(RunConfigurationFragmentLayout layout) {
            layout.addLabeled(RemoteServerLocalize.labelTextServer(), myServerCombo.getComponent());
            if (mySourceComboBox != null) {
                layout.addLabeled(RemoteServerLocalize.labelTextDeployment(), mySourceComboBox);
            }
            layout.addBottom(myDeploymentSettingsComponent);
        }

        private @Nullable DeploymentSource getSelectedSource() {
            return mySourceComboBox != null ? mySourceComboBox.getValue() : myLockedSource;
        }

        @RequiredUIAccess
        private void updateDeploymentSettingsEditor() {
            RemoteServer<S> selectedServer = myServerCombo.getSelectedServer();
            DeploymentSource selectedSource = getSelectedSource();
            if (Comparing.equal(selectedSource, myLastSelectedSource) && Comparing.equal(selectedServer, myLastSelectedServer)) {
                return;
            }

            if (!Comparing.equal(selectedSource, myLastSelectedSource)) {
                updateBeforeRunOptions(myLastSelectedSource, false);
                updateBeforeRunOptions(selectedSource, true);
            }

            myDeploymentSettingsEditor = null;
            myDeploymentSettingsComponent.removeAll();
            if (selectedSource != null && !Disposer.isDisposed(myParent)) {
                RunConfigurationFragmentEditor<D> editor = myDeploymentConfigurator.createEditor(selectedSource, selectedServer);
                myDeploymentSettingsEditor = editor;
                if (editor != null) {
                    myDeploymentSettingsComponent.center(buildRows(editor));
                    if (myDeploymentConfiguration != null) {
                        editor.reset(myDeploymentConfiguration);
                    }
                }
            }
            myLastSelectedSource = selectedSource;
            myLastSelectedServer = selectedServer;
        }

        @RequiredUIAccess
        private Component buildRows(RunConfigurationFragmentEditor<D> editor) {
            FormBuilder builder = FormBuilder.create();
            List<Component> bottom = new ArrayList<>();
            editor.addRows(new RunConfigurationFragmentLayout() {
                @RequiredUIAccess
                @Override
                public void addLabeled(LocalizeValue label, Component component) {
                    builder.addLabeled(label, component);
                }

                @RequiredUIAccess
                @Override
                public void addUnlabeled(Component component) {
                    bottom.add(component);
                }

                @RequiredUIAccess
                @Override
                public void addBottom(Component component) {
                    bottom.add(component);
                }
            });
            bottom.forEach(builder::addBottom);
            return builder.build();
        }

        @SuppressWarnings("unchecked")
        private void updateBeforeRunOptions(@Nullable DeploymentSource source, boolean selected) {
            if (source != null) {
                DeploymentSourceType type = source.getType();
                type.updateBuildBeforeRunOption(myServerCombo.getComponent(), myProject, source, selected);
            }
        }

        @SuppressWarnings("unchecked")
        @RequiredUIAccess
        @Override
        public void reset(RunConfiguration target) {
            DeployToServerRunConfiguration<S, D> configuration = (DeployToServerRunConfiguration<S, D>) target;
            myServerCombo.selectServerInCombo(configuration.getServerName());
            if (mySourceComboBox != null) {
                mySourceComboBox.setValue(configuration.getDeploymentSource(), false);
            }

            D deploymentConfiguration = configuration.getDeploymentConfiguration();
            myDeploymentConfiguration = deploymentConfiguration;
            updateDeploymentSettingsEditor();
            RunConfigurationFragmentEditor<D> editor = myDeploymentSettingsEditor;
            if (deploymentConfiguration != null && editor != null) {
                editor.reset(deploymentConfiguration);
            }
        }

        @SuppressWarnings("unchecked")
        @RequiredUIAccess
        @Override
        public void apply(RunConfiguration target) throws ConfigurationException {
            DeployToServerRunConfiguration<S, D> configuration = (DeployToServerRunConfiguration<S, D>) target;
            updateDeploymentSettingsEditor();
            myServerCombo.validateAutoDetectedItem();

            configuration.setServerName(Optional.ofNullable(myServerCombo.getSelectedServer()).map(RemoteServer::getName).orElse(null));
            DeploymentSource deploymentSource = getSelectedSource();
            configuration.setDeploymentSource(deploymentSource);

            if (deploymentSource == null) {
                configuration.setDeploymentConfiguration(null);
                return;
            }

            D deployment = configuration.getDeploymentConfiguration();
            if (deployment == null) {
                deployment = myDeploymentConfigurator.createDefaultConfiguration(deploymentSource);
                configuration.setDeploymentConfiguration(deployment);
            }
            myDeploymentConfiguration = deployment;
            RunConfigurationFragmentEditor<D> editor = myDeploymentSettingsEditor;
            if (editor != null) {
                editor.apply(deployment);
            }
        }
    }
}
