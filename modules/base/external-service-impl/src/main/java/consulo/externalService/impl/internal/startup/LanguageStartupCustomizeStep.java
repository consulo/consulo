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
package consulo.externalService.impl.internal.startup;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.update.PluginDownloader;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.localize.internal.LocalizeManagerEx;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Length;
import consulo.ui.RadioGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TileLayout;
import consulo.ui.style.StyleManager;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Offers the language pack of the language the system runs in, beside English. A pack chosen here is unpacked into the
 * plugins directory at once and its texts are read right away, so the following pages already show in that language -
 * the plugin itself loads at the next start.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class LanguageStartupCustomizeStep implements StartupCustomizeStep {
    private static final Logger LOG = Logger.getInstance(LanguageStartupCustomizeStep.class);

    private static final String LOCALIZATION_TAG = "localization";
    private static final String PACK_PREFIX = "consulo.localize_";

    private final @Nullable PluginDescriptor myPack;
    private final @Nullable Locale myPackLocale;

    private @Nullable Path myUnpackedPath;
    private @Nullable Label myStatusLabel;
    private @Nullable Component myComponent;
    private CompletableFuture<?> mySwitch = CompletableFuture.completedFuture(null);

    public LanguageStartupCustomizeStep(StartupCustomizeContext context) {
        Locale systemLocale = Locale.getDefault();

        PluginDescriptor pack = null;
        Locale packLocale = null;
        if (!Locale.ENGLISH.getLanguage().equals(systemLocale.getLanguage())) {
            for (PluginDescriptor descriptor : context.getRepositoryPlugins()) {
                Locale locale = packLocale(descriptor);
                if (locale == null || !locale.getLanguage().equals(systemLocale.getLanguage())) {
                    continue;
                }

                boolean sameCountry = locale.getCountry().equals(systemLocale.getCountry());
                if (pack == null || sameCountry) {
                    pack = descriptor;
                    packLocale = locale;
                }
            }
        }

        myPack = pack;
        myPackLocale = packLocale;
    }

    private static @Nullable Locale packLocale(PluginDescriptor descriptor) {
        String id = descriptor.getPluginId().getIdString();
        if (!id.startsWith(PACK_PREFIX) || !descriptor.getTags().contains(LOCALIZATION_TAG)) {
            return null;
        }

        String[] parts = id.substring(PACK_PREFIX.length()).split("_");
        return parts.length > 1 ? Locale.of(parts[0], parts[1]) : Locale.of(parts[0]);
    }

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizeLanguageTitle();
    }

    @Override
    public boolean isVisible(StartupCustomizeContext context) {
        return myPack != null && myPackLocale != null;
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        Locale packLocale = myPackLocale == null ? Locale.US : myPackLocale;

        RadioGroup<Locale> group = RadioGroup.create();
        TileLayout tiles = TileLayout.create(Length.ofFont(12));
        for (Locale locale : List.of(Locale.US, packLocale)) {
            tiles.add(StartupCustomizeUI.choiceCard(group, locale, LocalizeValue.of(title(locale)), LocalizeValue.of(subtitle(locale)), null));
        }
        group.setValue(LocalizeManager.get().getLocale().getLanguage().equals(packLocale.getLanguage()) ? packLocale : Locale.US, false);
        group.addValueListener(locale -> {
            if (locale != null) {
                switchTo(locale);
            }
        });

        Label status = Label.create(LocalizeValue.empty());
        myStatusLabel = status;

        DockLayout content = DockLayout.create();
        content.top(tiles);
        content.bottom(status);

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizeLanguageHeader(), content);
        myComponent = component;
        return component;
    }

    private static String title(Locale locale) {
        String name = locale.getDisplayLanguage(locale);
        return name.isEmpty() ? locale.toString() : name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }

    private static String subtitle(Locale locale) {
        String country = locale.getDisplayCountry(locale);
        return country.isEmpty() ? locale.getDisplayLanguage(Locale.ENGLISH) : country;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> validateStep(StartupCustomizeContext context) {
        return mySwitch;
    }

    @RequiredUIAccess
    private void switchTo(Locale locale) {
        PluginDescriptor pack = myPack;
        UIAccess uiAccess = UIAccess.current();

        if (pack == null || locale.equals(Locale.US)) {
            Path unpacked = myUnpackedPath;
            if (unpacked != null) {
                myUnpackedPath = null;
                FileUtil.delete(unpacked.toFile());
            }

            apply(Locale.US, uiAccess);
            mySwitch = CompletableFuture.completedFuture(null);
            return;
        }

        if (myUnpackedPath != null || PluginManager.findPlugin(pack.getPluginId()) != null) {
            apply(locale, uiAccess);
            mySwitch = CompletableFuture.completedFuture(null);
            return;
        }

        setStatus(ExternalServiceLocalize.startupCustomizeLanguageDownloading());
        CompletableFuture<?> download = CompletableFuture.supplyAsync(() -> unpack(pack), AppExecutorUtil.getAppExecutorService())
            .thenAccept(path -> {
                myUnpackedPath = path;
                LocalizeManager.get().setLocale(locale, true);
            });
        download.whenComplete((ignored, error) -> {
            Label label = myStatusLabel;
            UIAccess labelAccess = label == null ? null : label.getUIAccess();
            if (labelAccess == null) {
                return;
            }

            labelAccess.give(() -> {
                setStatus(LocalizeValue.empty());
                if (error == null) {
                    StyleManager.get().forceRepaintAll();
                }
            });
        });
        mySwitch = download;
    }

    @RequiredUIAccess
    private static void apply(Locale locale, UIAccess uiAccess) {
        LocalizeManager.get().setLocale(locale, true);

        uiAccess.give(StyleManager.get()::forceRepaintAll);
    }

    @RequiredUIAccess
    private void setStatus(LocalizeValue text) {
        Label label = myStatusLabel;
        if (label != null) {
            label.setText(text);
        }
    }

    private static Path unpack(PluginDescriptor pack) {
        try {
            PluginDownloader downloader = PluginDownloader.createDownloader(pack, false);
            downloader.download(new EmptyProgressIndicator());

            Path path = downloader.installNow();
            if (!((LocalizeManagerEx) LocalizeManager.get()).loadPluginIndex(path)) {
                LOG.warn("Language pack without an index, its texts show after the restart: " + path);
            }
            return path;
        }
        catch (Exception e) {
            throw new CompletionException(e);
        }
    }
}
