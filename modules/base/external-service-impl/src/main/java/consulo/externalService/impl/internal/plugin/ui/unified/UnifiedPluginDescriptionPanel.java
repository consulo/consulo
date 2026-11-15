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
import consulo.application.internal.plugin.DynamicPluginManager;
import consulo.application.plugin.PluginActionListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionMarkup;
import consulo.externalService.impl.internal.plugin.ui.action.InstallPluginAction;
import consulo.externalService.impl.internal.plugin.ui.action.UninstallPluginAction;
import consulo.externalService.impl.internal.update.PlatformOrPluginInstallProcess;
import consulo.externalService.internal.PluginIconHolder;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginDescriptionPanel {
    private static final Logger LOG = Logger.getInstance(UnifiedPluginDescriptionPanel.class);

    private static final String DESCRIPTION_CSS = """
        body { margin: 5px 7px 0 7px; font-size: 12px; }
        .title { font-size: 13px; font-weight: bold; margin: 10px 0 10px 0; }
        .description { margin: 12px 0 2px 0; }
        """;

    private final Supplier<List<PluginDescriptor>> myAllPluginsGetter;

    private final DockLayout myRoot;
    private final VerticalLayout myHeader;
    private final Label myIconLabel;
    private final Label myNameLabel;
    private final Button myActionButton;
    private final Label myExperimentalLabel;
    private final Label myDownloadsLabel;
    private final HtmlView myDescription;
    private final TabbedLayout myTabs;
    private final UnifiedPluginDetailsPanel myDetails;

    private @Nullable PluginDescriptor myPlugin;
    private @Nullable Runnable myButtonAction;

    private final Application myApplication;

    @RequiredUIAccess
    public UnifiedPluginDescriptionPanel(
        Application application,
        @RequiredUIAccess Consumer<PluginId> pluginSelector,
        @RequiredUIAccess Consumer<String> tagSelector,
        Supplier<List<PluginDescriptor>> allPluginsGetter
    ) {
        myApplication = application;
        myAllPluginsGetter = allPluginsGetter;

        myIconLabel = Label.create(LocalizeValue.empty());

        myNameLabel = Label.create(LocalizeValue.empty());
        myNameLabel.addStyle(LabelStyle.FONT_LARGE);
        myNameLabel.addStyle(LabelStyle.FONT_BOLD);
        myNameLabel.paddingBuilder().leftSet(Space.LARGE).apply();

        myActionButton = Button.create(LocalizeValue.empty(), event -> {
            Runnable action = myButtonAction;
            if (action != null) {
                action.run();
            }
        });
        myActionButton.setVisible(false);

        DockLayout nameLine = DockLayout.create(Space.NONE);
        nameLine.left(myIconLabel);
        nameLine.center(myNameLabel);
        nameLine.right(VerticalLayout.create(Space.NONE, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER).add(myActionButton));

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
        myHeader.add(DockLayout.create(Space.NONE).right(myExperimentalLabel));
        myHeader.add(DockLayout.create(Space.NONE).right(myDownloadsLabel));
        myHeader.paddingBuilder().topSet(Space.X_LARGE).horizontalSet(Space.X_LARGE).apply();
        myHeader.setVisible(false);

        myDescription = createHtmlView(pluginSelector);
        myDetails = new UnifiedPluginDetailsPanel(pluginSelector, tagSelector);

        myTabs = TabbedLayout.create();
        myTabs.addTab(LocalizeValue.localizeTODO("Overview"), myDescription);
        myTabs.addTab(LocalizeValue.localizeTODO("Features"), createNotAvailable());
        myTabs.addTab(LocalizeValue.localizeTODO("Comments"), createNotAvailable());
        myTabs.setVisible(false);

        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(myHeader);
        myRoot.center(myTabs);
        myRoot.setBackgroundColor(ComponentColors.LAYOUT);
    }

    /**
     * The header with the action button, the description and the change notes.
     */
    public Component getInfoComponent() {
        return myRoot;
    }

    /**
     * Version, vendor, dependencies and the other facts of the plugin.
     */
    public Component getDetailsComponent() {
        return myDetails.getComponent();
    }

    @RequiredUIAccess
    private static Component createNotAvailable() {
        Label label = Label.create(
            LocalizeValue.localizeTODO("Not available yet"),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()
        );
        label.setForegroundColor(ComponentColors.DISABLED_TEXT);
        return DockLayout.create(Space.NONE).center(label);
    }

    @RequiredUIAccess
    private static HtmlView createHtmlView(@RequiredUIAccess Consumer<PluginId> pluginSelector) {
        HtmlView view = HtmlView.create();
        view.addHyperlinkListener(event -> {
            String description = event.getDescription();
            if (description.startsWith(PluginDescriptionMarkup.PLUGIN_PREFIX)) {
                pluginSelector.accept(PluginId.getId(description.substring(PluginDescriptionMarkup.PLUGIN_PREFIX.length())));
            }
            else if (!description.isEmpty()) {
                Platform.current().openInBrowser(description);
            }
        });
        view.setBackgroundColor(ComponentColors.LAYOUT);
        return view;
    }

    @RequiredUIAccess
    public void update(@Nullable PluginDescriptor plugin, List<PluginDescriptor> allPlugins) {
        myPlugin = plugin;

        if (plugin == null) {
            myHeader.setVisible(false);
            myTabs.setVisible(false);
            render(myDescription, "");
            myDetails.update(null, allPlugins);
            return;
        }

        myHeader.setVisible(true);
        myTabs.setVisible(true);
        myIconLabel.setImage(PluginIconHolder.getLarge(plugin));
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

        render(myDescription, PluginDescriptionMarkup.buildDescription(plugin).toString());
        myDetails.update(plugin, allPlugins);
    }

    @RequiredUIAccess
    private static void render(HtmlView view, String body) {
        view.render(new HtmlView.RenderData("<html><body>" + body + "</body></html>", DESCRIPTION_CSS, new URL[0]));
    }

    @RequiredUIAccess
    private void updateButton(PluginDescriptor plugin) {
        PluginId pluginId = plugin.getPluginId();
        InstalledPluginsState state = InstalledPluginsState.getInstance();

        boolean touchedInSession = state.getInstalledPlugins().contains(pluginId)
            || state.wasUpdated(pluginId)
            || plugin.isDeleted();

        if (touchedInSession) {
            LocalizeValue text = myApplication.isRestartCapable()
                ? ExternalServiceLocalize.actionRestartText()
                : ExternalServiceLocalize.actionShutdownText();
            showButton(text, PlatformIconGroup.actionsRestart(), () -> {
                PlatformOrPluginInstallProcess.askRestart(myApplication, myRoot);
            });
        }
        else if (PluginManager.findPlugin(pluginId) != null) {
            if (PluginIds.isPlatformPlugin(pluginId)) {
                myActionButton.setVisible(false);
            }
            else {
                showButton(ExternalServiceLocalize.actionUninstallPlugin(), PlatformIconGroup.actionsCancel(), () -> uninstall(myApplication, myRoot, plugin));
            }
        }
        else if (PluginManager.findPlugin(pluginId) == null) {
            showButton(LocalizeValue.localizeTODO("Install"), PlatformIconGroup.actionsInstall(), () -> install(myApplication, myRoot, plugin));
        }
        else {
            myActionButton.setVisible(false);
        }
    }

    @RequiredUIAccess
    private void showButton(LocalizeValue text, Image icon, @RequiredUIAccess Runnable action) {
        myActionButton.setText(text);
        myActionButton.setIcon(icon);
        myButtonAction = action;
        myActionButton.setVisible(true);
        myActionButton.setEnabled(true);
    }

    @RequiredUIAccess
    private void install(Application application, Component component, PluginDescriptor plugin) {
        UIAccess uiAccess = UIAccess.current();

        InstallPluginAction.install(application, null, component, plugin, myAllPluginsGetter.get(), false, installed -> {
            if (installed.isEmpty()) {
                return;
            }

            List<PluginDescriptor> waitingRestart = new ArrayList<>();
            for (PluginDescriptor descriptor : installed) {
                if (PluginInstallUtil.isRestartRequired(List.of(descriptor))) {
                    waitingRestart.add(descriptor);
                }
            }
            PluginInstallUtil.markInstalled(waitingRestart);

            uiAccess.give(() -> {
                if (myPlugin == plugin) {
                    updateButton(plugin);
                }

                if (!waitingRestart.isEmpty()) {
                    PlatformOrPluginInstallProcess.askRestart(application, component);
                }
            });
        });
    }

    @RequiredUIAccess
    private void uninstall(Application application, Component component, PluginDescriptor plugin) {
        MessageBoxes.yesNo()
            .asQuestion()
            .title(ExternalServiceLocalize.titlePluginUninstall())
            .text(ExternalServiceLocalize.promptUninstallPlugin(plugin.getName()))
            .showAsync(component)
            .whenCompleteAsync((agreed, error) -> {
                if (Boolean.TRUE.equals(agreed)) {
                    checkDependentAndUninstall(application, component, plugin);
                }
            }, UIAccess.current());
    }

    @RequiredUIAccess
    private void checkDependentAndUninstall(Application application, Component component, PluginDescriptor plugin) {
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
                .showAsync(component)
                .whenCompleteAsync((agreed, error) -> {
                    if (Boolean.TRUE.equals(agreed)) {
                        doUninstall(application, component, plugin);
                    }
                }, UIAccess.current());
            return;
        }

        doUninstall(application, component, plugin);
    }

    @RequiredUIAccess
    private void doUninstall(Application application, Component component, PluginDescriptor plugin) {
        DynamicPluginManager dynamicPluginManager = application.getInstance(DynamicPluginManager.class);
        if (!dynamicPluginManager.isEnabled()) {
            doUninstallOnRestart(application, component, plugin);
            return;
        }

        PluginId pluginId = plugin.getPluginId();
        CompletableFuture.supplyAsync(
                () -> dynamicPluginManager.createSession().uninstall(pluginId).apply(),
                AppExecutorUtil.getAppExecutorService()
            )
            .whenCompleteAsync((result, error) -> {
                if (error != null) {
                    LOG.error(error);
                }

                if (result != null && result.getUninstalled().contains(pluginId)) {
                    application.getMessageBus()
                        .syncPublisher(PluginActionListener.class)
                        .pluginsUninstalled(new PluginId[]{pluginId});
                }
                else {
                    doUninstallOnRestart(application, component, plugin);
                }
            }, UIAccess.current());
    }

    @RequiredUIAccess
    private void doUninstallOnRestart(Application application, Component component, PluginDescriptor plugin) {
        if (!UninstallPluginAction.uninstallPlugin(plugin)) {
            return;
        }

        application.getMessageBus()
            .syncPublisher(PluginActionListener.class)
            .pluginsUninstalled(new PluginId[]{plugin.getPluginId()});

        if (myPlugin == plugin) {
            updateButton(plugin);
        }

        PlatformOrPluginInstallProcess.askRestart(application, component);
    }
}
