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
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionMarkup;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginAction;
import consulo.externalService.impl.internal.plugin.ui.action.UninstallPluginAction;
import consulo.externalService.impl.internal.update.PlatformOrPluginInstallProcess;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginDescriptionPanel {
    private static final String DESCRIPTION_CSS = """
        body { margin: 5px 7px 0 7px; font-size: 12px; }
        .title { font-size: 13px; font-weight: bold; margin: 10px 0 10px 0; }
        .description { margin: 12px 0 2px 0; }
        """;

    private final boolean myInstalledTab;
    private final Supplier<List<PluginDescriptor>> myAllPluginsGetter;

    private final DockLayout myRoot;
    private final VerticalLayout myHeader;
    private final Label myIconLabel;
    private final Label myNameLabel;
    private final Button myActionButton;
    private final Label myExperimentalLabel;
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

        myIconLabel = Label.create(LocalizeValue.empty());

        myNameLabel = Label.create(LocalizeValue.empty());
        myNameLabel.addStyle(LabelStyle.FONT_LARGE);
        myNameLabel.addStyle(LabelStyle.FONT_BOLD);
        myNameLabel.paddingBuilder().leftSet(Space.LARGE).apply();

        DockLayout nameLine = DockLayout.create(Space.NONE);
        nameLine.left(myIconLabel);
        nameLine.center(myNameLabel);

        myActionButton = Button.create(LocalizeValue.empty(), event -> {
            Runnable action = myButtonAction;
            if (action != null) {
                action.run();
            }
        });
        myActionButton.setVisible(false);

        myExperimentalLabel = Label.create(LocalizeValue.localizeTODO("Experimental"));
        myExperimentalLabel.setImage(PlatformIconGroup.generalBalloonwarning());
        myExperimentalLabel.addStyle(LabelStyle.FONT_LARGE);
        myExperimentalLabel.addStyle(LabelStyle.FONT_BOLD);
        myExperimentalLabel.setForegroundColor(StandardColors.RED);
        myExperimentalLabel.setVisible(false);

        myDownloadsLabel = Label.create(LocalizeValue.empty());
        myDownloadsLabel.addStyle(LabelStyle.FONT_X_SMALL);

        myHeader = VerticalLayout.create(Space.SMALL);
        myHeader.add(nameLine);
        myHeader.add(DockLayout.create(Space.NONE).right(myActionButton));
        myHeader.add(DockLayout.create(Space.NONE).right(myExperimentalLabel));
        myHeader.add(DockLayout.create(Space.NONE).right(myDownloadsLabel));
        myHeader.paddingBuilder().topSet(Space.SMALL).horizontalSet(Space.SMALL).apply();
        myHeader.setVisible(false);

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

        myDescription.setBackgroundColor(ComponentColors.LAYOUT);

        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(myHeader);
        myRoot.center(myDescription);
        myRoot.setBackgroundColor(ComponentColors.LAYOUT);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void update(@Nullable PluginDescriptor plugin, List<PluginDescriptor> allPlugins) {
        myPlugin = plugin;

        if (plugin == null) {
            myHeader.setVisible(false);
            render("");
            return;
        }

        myHeader.setVisible(true);
        myIconLabel.setImage(PluginIconHolder.get(plugin));
        myNameLabel.setText(LocalizeValue.of(StringUtil.notNullize(plugin.getName())));
        myExperimentalLabel.setVisible(plugin.isExperimental());

        if (plugin instanceof PluginNode pluginNode) {
            myDownloadsLabel.setText(LocalizeValue.localizeTODO(pluginNode.getDownloads() + " downloads"));
            myDownloadsLabel.setVisible(true);
        }
        else {
            myDownloadsLabel.setVisible(false);
        }

        updateButton(plugin);

        render(PluginDescriptionMarkup.buildBody(plugin, allPlugins).toString());
    }

    @RequiredUIAccess
    private void render(String body) {
        myDescription.render(new HtmlView.RenderData("<html><body>" + body + "</body></html>", DESCRIPTION_CSS, new URL[0]));
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
            showButton(text, PlatformIconGroup.actionsRestart(), PlatformOrPluginInstallProcess::askRestart);
        }
        else if (myInstalledTab) {
            if (PluginIds.isPlatformPlugin(pluginId)) {
                myActionButton.setVisible(false);
            }
            else {
                showButton(ExternalServiceLocalize.actionUninstallPlugin(), PlatformIconGroup.actionsCancel(), () -> uninstall(plugin));
            }
        }
        else if (PluginManager.findPlugin(pluginId) == null) {
            showButton(LocalizeValue.localizeTODO("Install"), PlatformIconGroup.actionsInstall(), () -> install(plugin));
        }
        else {
            myActionButton.setVisible(false);
        }
    }

    @RequiredUIAccess
    private void showButton(LocalizeValue text, Image icon, Runnable action) {
        myActionButton.setText(text);
        myActionButton.setIcon(icon);
        myButtonAction = action;
        myActionButton.setVisible(true);
        myActionButton.setEnabled(true);
    }

    @RequiredUIAccess
    private void install(PluginDescriptor plugin) {
        UIAccess uiAccess = UIAccess.current();

        InstallPluginAction.install(null, plugin, myAllPluginsGetter.get(), false, installed -> {
            if (installed.isEmpty()) {
                return;
            }

            PluginInstallUtil.markInstalled(installed);

            uiAccess.give(() -> {
                if (myPlugin == plugin) {
                    updateButton(plugin);
                }

                PlatformOrPluginInstallProcess.askRestart();
            });
        });
    }

    @RequiredUIAccess
    private void uninstall(PluginDescriptor plugin) {
        MessageBoxes.yesNo()
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
            MessageBoxes.yesNo()
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
        if (!UninstallPluginAction.uninstallPlugin(plugin)) {
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
