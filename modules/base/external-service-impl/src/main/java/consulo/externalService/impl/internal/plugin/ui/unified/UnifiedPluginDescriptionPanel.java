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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.application.Application;
import consulo.application.plugin.PluginActionListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionMarkup;
import consulo.externalService.impl.internal.plugin.ui.action.UninstallPluginAction;
import consulo.externalService.impl.internal.update.PlatformOrPluginInstallProcess;
import consulo.externalService.impl.internal.update.PlatformOrPluginNode;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateResult;
import consulo.externalService.impl.internal.update.UnifiedPlatformOrPluginDialog;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginDescriptionPanel {
    private final boolean myInstalledTab;
    private final Supplier<List<PluginDescriptor>> myAllPluginsGetter;

    private final DockLayout myRoot;
    private final Label myNameLabel;
    private final Label myVersionLabel;
    private final Button myActionButton;
    private final Label myDownloadsLabel;
    private final HtmlView myDescription;

    private @Nullable PluginDescriptor myPlugin;
    private @Nullable Runnable myButtonAction;

    @RequiredUIAccess
    public UnifiedPluginDescriptionPanel(
        boolean installedTab,
        Consumer<PluginId> pluginSelector,
        Supplier<List<PluginDescriptor>> allPluginsGetter
    ) {
        myInstalledTab = installedTab;
        myAllPluginsGetter = allPluginsGetter;

        myNameLabel = Label.create(LocalizeValue.empty());
        myVersionLabel = Label.create(LocalizeValue.empty());

        HorizontalLayout titleLine = HorizontalLayout.create(5);
        titleLine.add(myNameLabel);
        titleLine.add(myVersionLabel);

        myActionButton = Button.create(LocalizeValue.empty(), event -> {
            Runnable action = myButtonAction;
            if (action != null) {
                action.run();
            }
        });
        myActionButton.setVisible(false);

        myDownloadsLabel = Label.create(LocalizeValue.empty());
        myDownloadsLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        HorizontalLayout actionLine = HorizontalLayout.create(8);
        actionLine.add(myActionButton);
        actionLine.add(myDownloadsLabel);

        DockLayout header = DockLayout.create(0);
        header.center(titleLine);
        header.right(actionLine);
        header.addBorders(BorderStyle.EMPTY, null, 5);

        myDescription = HtmlView.create();
        myDescription.addHyperlinkListener(event -> {
            String description = event.getDescription();
            if (description.startsWith(PluginDescriptionMarkup.PLUGIN_PREFIX)) {
                pluginSelector.accept(PluginId.getId(description.substring(PluginDescriptionMarkup.PLUGIN_PREFIX.length())));
            }
            else if (!description.isEmpty()) {
                Platform.current().openInBrowser(description);
            }
        });

        myRoot = DockLayout.create(0);
        myRoot.top(header);
        myRoot.center(ScrollableLayout.create(myDescription));
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void update(@Nullable PluginDescriptor plugin, List<PluginDescriptor> allPlugins) {
        myPlugin = plugin;

        if (plugin == null) {
            myNameLabel.setImage(null);
            myNameLabel.setText(LocalizeValue.empty());
            myVersionLabel.setText(LocalizeValue.empty());
            myActionButton.setVisible(false);
            myDownloadsLabel.setText(LocalizeValue.empty());
            myDescription.render(new HtmlView.RenderData("<html><body></body></html>"));
            return;
        }

        myNameLabel.setImage(PluginIconHolder.get(plugin));
        myNameLabel.setText(LocalizeValue.of(StringUtil.notNullize(plugin.getName())));
        myVersionLabel.setText(LocalizeValue.of(StringUtil.notNullize(plugin.getVersion())));

        if (plugin instanceof PluginNode pluginNode && pluginNode.getDownloads() > 0) {
            myDownloadsLabel.setText(LocalizeValue.localizeTODO(pluginNode.getDownloads() + " downloads"));
        }
        else {
            myDownloadsLabel.setText(LocalizeValue.empty());
        }

        updateButton(plugin);

        StringBuilder body = PluginDescriptionMarkup.buildBody(plugin, allPlugins);
        myDescription.render(new HtmlView.RenderData("<html><body>" + body + "</body></html>"));
    }

    @RequiredUIAccess
    private void updateButton(PluginDescriptor plugin) {
        PluginId pluginId = plugin.getPluginId();
        InstalledPluginsState state = InstalledPluginsState.getInstance();

        boolean touchedInSession = state.getInstalledPlugins().contains(pluginId)
            || state.wasUpdated(pluginId)
            || plugin.isDeleted();

        if (touchedInSession) {
            LocalizeValue text = Application.get().isRestartCapable()
                ? ExternalServiceLocalize.actionRestartText()
                : ExternalServiceLocalize.actionShutdownText();
            showButton(text, PlatformOrPluginInstallProcess::askRestart);
        }
        else if (myInstalledTab) {
            showButton(ExternalServiceLocalize.actionUninstallPlugin(), () -> uninstall(plugin));
        }
        else if (PluginManager.findPlugin(pluginId) == null) {
            showButton(LocalizeValue.localizeTODO("Install"), () -> install(plugin));
        }
        else {
            myActionButton.setVisible(false);
        }
    }

    @RequiredUIAccess
    private void showButton(LocalizeValue text, Runnable action) {
        myActionButton.setText(text);
        myButtonAction = action;
        myActionButton.setVisible(true);
        myActionButton.setEnabled(true);
    }

    @RequiredUIAccess
    private void install(PluginDescriptor plugin) {
        if (plugin.isExperimental()) {
            Alerts.yesNo()
                .asWarning()
                .title(Application.get().getName())
                .text(LocalizeValue.localizeTODO(
                    "Are you sure install experimental plugin? Plugin can make IDE unstable, and may not implement expected features"))
                .showAsync()
                .whenComplete((agreed, error) -> {
                    if (Boolean.TRUE.equals(agreed)) {
                        doInstall(plugin);
                    }
                });
            return;
        }

        doInstall(plugin);
    }

    @RequiredUIAccess
    private void doInstall(PluginDescriptor plugin) {
        UIAccess uiAccess = UIAccess.current();

        List<PluginDescriptor> allPlugins = myAllPluginsGetter.get();

        Set<PluginDescriptor> withDependencies = PluginInstallUtil.getPluginsForInstall(List.of(plugin), allPlugins);

        List<PlatformOrPluginNode> nodes = withDependencies.stream()
            .map(descriptor -> new PlatformOrPluginNode(descriptor.getPluginId(), null, descriptor))
            .collect(Collectors.toList());
        PlatformOrPluginUpdateResult result = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.PLUGIN_INSTALL, nodes);

        // the picked plugin is the target, only its dependencies are drawn as new arrivals
        Predicate<PluginId> greenStrategy = pluginId -> !pluginId.equals(plugin.getPluginId());

        Consumer<Collection<PluginDescriptor>> afterCallback = installed -> {
            if (installed.isEmpty()) {
                return;
            }

            InstalledPluginsState state = InstalledPluginsState.getInstance();
            for (PluginDescriptor descriptor : installed) {
                state.getInstalledPlugins().add(descriptor.getPluginId());
                state.getOutdatedPlugins().remove(descriptor.getPluginId());

                PluginDescriptor alreadyInstalled = PluginManager.findPlugin(descriptor.getPluginId());
                if (alreadyInstalled != null) {
                    state.updateExistingPlugin(descriptor, alreadyInstalled);
                }
                else {
                    // the installed list is built from what the platform loaded plus what this session brought
                    state.getAllPlugins().add(descriptor);
                }
            }

            uiAccess.give(() -> {
                if (myPlugin == plugin) {
                    updateButton(plugin);
                }

                PlatformOrPluginInstallProcess.askRestart();
            });
        };

        UnifiedPlatformOrPluginDialog dialog = new UnifiedPlatformOrPluginDialog(null, result, greenStrategy, afterCallback, false);
        if (withDependencies.size() == 1) {
            dialog.doOKAction();
        }
        else {
            dialog.show();
        }
    }

    @RequiredUIAccess
    private void uninstall(PluginDescriptor plugin) {
        Alerts.yesNo()
            .asQuestion()
            .title(ExternalServiceLocalize.titlePluginUninstall())
            .text(ExternalServiceLocalize.promptUninstallPlugin(plugin.getName()))
            .showAsync()
            .whenComplete((agreed, error) -> {
                if (Boolean.TRUE.equals(agreed)) {
                    checkDependentAndUninstall(plugin);
                }
            });
    }

    @RequiredUIAccess
    private void checkDependentAndUninstall(PluginDescriptor plugin) {
        List<PluginDescriptor> dependent = ContainerUtil.filter(
            myAllPluginsGetter.get(),
            descriptor -> !descriptor.isDeleted()
                && (ArrayUtil.contains(plugin.getPluginId(), descriptor.getDependentPluginIds())
                || ArrayUtil.contains(plugin.getPluginId(), descriptor.getOptionalDependentPluginIds()))
        );

        if (!dependent.isEmpty()) {
            Alerts.yesNo()
                .asQuestion()
                .title(ExternalServiceLocalize.titlePluginUninstall())
                .text(ExternalServiceLocalize.severalPluginsDependOn0ContinueToRemove(plugin.getName()))
                .showAsync()
                .whenComplete((agreed, error) -> {
                    if (Boolean.TRUE.equals(agreed)) {
                        doUninstall(plugin);
                    }
                });
            return;
        }

        doUninstall(plugin);
    }

    @RequiredUIAccess
    private void doUninstall(PluginDescriptor plugin) {
        if (!UninstallPluginAction.uninstallPlugin(plugin, null)) {
            return;
        }

        Application.get().getMessageBus()
            .syncPublisher(PluginActionListener.class)
            .pluginsUninstalled(new PluginId[]{plugin.getPluginId()});

        if (myPlugin == plugin) {
            updateButton(plugin);
        }

        PlatformOrPluginInstallProcess.askRestart();
    }
}
