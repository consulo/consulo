// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.application.plugin.PluginActionListener;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.component.messagebus.MessageBusConnection;
import consulo.container.plugin.PluginId;
import consulo.desktop.awt.os.mac.internal.NSDefaults;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.jna.JnaLoader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;

public final class TouchbarSupport {
    private static final String IS_ENABLED_KEY = "ide.mac.touchbar.enabled";
    private static final Logger LOG = Logger.getInstance(TouchbarSupport.class);
    private static final @Nonnull AWTEventListener ourAWTEventListener = e -> {
        TouchBarsManager.processAWTEvent(e);
    };
    private static final long ourEventMask = AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK;

    private static volatile boolean isInitialized;
    private static volatile boolean isEnabled = false;

    private static MessageBusConnection ourConnection;

    private static void initialize() {
        if (isInitialized) {
            return;
        }

        synchronized (TouchbarSupport.class) {
            if (isInitialized) {
                return;
            }

            try {
                if (GraphicsEnvironment.isHeadless()) {
                    LOG.info("touchbar disabled: the graphics environment is headless");
                }
                else if (!Registry.is(IS_ENABLED_KEY, true)) {
                    LOG.info("touchbar disabled: registry");
                }
                else if (!JnaLoader.isLoaded()) {
                    LOG.info("touchbar disabled: JNA library is unavailable");
                }
                else if (!Helpers.isTouchBarServerRunning()) {
                    LOG.info("touchbar disabled: touchbar-server isn't running");
                }
                else {
                    isEnabled = true;

                    // read isEnabled from OS (i.e., NSDefaults)
                    String appId = Helpers.getAppId();
                    if (appId == null || appId.isEmpty()) {
                        LOG.info("can't obtain application id from NSBundle (touchbar enabled)");
                    }
                    else if (NSDefaults.isShowFnKeysEnabled(appId)) {
                        // user has enabled the setting "FN-keys in touchbar" (global or per-app)
                        if (NSDefaults.isFnShowsAppControls()) {
                            LOG.info("touchbar enabled: show FN-keys but pressing fn-key toggle to show app-controls");
                        }
                        else {
                            LOG.info("touchbar disabled: show fn-keys");
                            isEnabled = false;
                        }
                    }
                    else {
                        LOG.info("touchbar support is enabled");
                    }
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }

            if (!isEnabled) {
                isInitialized = true;
                return;
            }

            try {
                NST.loadLibrary();
            }
            catch (Throwable e) {
                LOG.error(e);
                isEnabled = false;
            }

            isInitialized = true;
            enableSupport();
        }
    }

    private static void enableSupport() {
        isEnabled = true;

        // initialize keyboard listener
        Toolkit.getDefaultToolkit().addAWTEventListener(ourAWTEventListener, ourEventMask);

        // initialize default and tool-window contexts
        CtxDefault.initialize();
        CtxToolWindows.initialize();

        // listen plugins
        ourConnection = ApplicationManager.getApplication().getMessageBus().connect();
        ourConnection.subscribe(PluginActionListener.class, new PluginActionListener() {
            @Override
            public void pluginsInstalled(@Nonnull PluginId[] pluginIds) {
                reloadAllActions();
            }

            @Override
            public void pluginsUninstalled(@Nonnull PluginId[] pluginIds) {
                reloadAllActions();
            }
        });
    }

    public static void enable(boolean enable) {
        if (!isInitialized || !isAvailable()) {
            return;
        }

        if (!enable) {
            if (isEnabled) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(ourAWTEventListener);

                TouchBarsManager.clearAll();
                CtxDefault.disable();
                CtxToolWindows.disable();

                if (ourConnection != null) {
                    ourConnection.disconnect();
                }
                ourConnection = null;

                isEnabled = false;
            }
            return;
        }

        if (!isEnabled && Registry.is(IS_ENABLED_KEY)) {
            enableSupport();
        }
    }

    public static void onApplicationLoaded() {
        initialize();
    }

    public static boolean isAvailable() {
        return SystemInfo.isMac && NST.isAvailable();
    }

    public static boolean isEnabled() {
        return isAvailable() && isEnabled && Registry.is(IS_ENABLED_KEY);
    }

    public static void onUpdateEditorHeader(@Nonnull Editor editor) {
        if (!isInitialized || !isEnabled()) {
            return;
        }

        CtxEditors.onUpdateEditorHeader(editor);
    }

    public static void showPopupItems(@Nonnull JBPopup popup, @Nonnull JComponent popupComponent) {
        if (!isInitialized || !isEnabled()) {
            return;
        }
        final Disposable tb = CtxPopup.showPopupItems(popup, popupComponent);
        if (tb != null) {
            Disposer.register(popup, tb);
        }
    }

    public static @Nullable Disposable showWindowActions(@Nonnull Component contentPane) {
        if (!isInitialized || !isEnabled()) {
            return null;
        }

        return CtxDialogs.showWindowActions(contentPane);
    }

    public static void showWindowActions(@Nonnull Disposable parent, @Nonnull Component contentPane) {
        Disposable tb = showWindowActions(contentPane);
        if (tb != null) {
            Disposer.register(parent, tb);
        }
    }

    public static void reloadAllActions() {
        if (!isInitialized || !isEnabled()) {
            return;
        }

        CtxDefault.reloadAllActions();
        CtxToolWindows.reloadAllActions();
    }
}
