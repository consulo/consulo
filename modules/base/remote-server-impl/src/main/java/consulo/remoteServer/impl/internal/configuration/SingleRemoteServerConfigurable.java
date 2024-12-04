// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration;

import consulo.configurable.ConfigurationException;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.RemoteServerConfigurable;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.impl.internal.util.DelayedRunner;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class SingleRemoteServerConfigurable extends NamedConfigurable<RemoteServer<?>> {
    private final RemoteServerConfigurable myConfigurable;
    private final RemoteServer<?> myServer;
    private String myServerName;
    private boolean myNew;
    private JPanel myMainPanel;
    private JPanel mySettingsPanel;
    private JBLabel myConnectionStatusLabel;

    private final DelayedRunner myRunner;
    private ConnectionTester myConnectionTester;

    private final RemoteServer<?> myInnerServer;
    private boolean myInnerApplied;
    private boolean myAppliedButNeedsCheck;

    private boolean myConnected;

    public <C extends ServerConfiguration> SingleRemoteServerConfigurable(RemoteServer<C> server, Runnable treeUpdater, boolean isNew) {
        super(true, treeUpdater);
        myServer = server;
        myNew = isNew;
        myServerName = myServer.getName();
        C configuration = server.getConfiguration();
        C innerConfiguration = XmlSerializerUtil.createCopy(configuration);
        myInnerServer = new RemoteServerImpl<>("<temp inner server>", server.getType(), innerConfiguration);
        myInnerApplied = false;
        myAppliedButNeedsCheck = isNew || server.getType().canAutoDetectConfiguration();

        myConfigurable = createConfigurable(server, innerConfiguration);

        myConnected = false;
        myRunner = new DelayedRunner(myMainPanel) {

            @Override
            protected boolean wasChanged() {
                if (!myConfigurable.canCheckConnection()) {
                    return false;
                }

                boolean modified = myConfigurable.isModified();
                boolean result = modified || myAppliedButNeedsCheck;
                if (result) {
                    myAppliedButNeedsCheck = false;

                    setConnectionStatus(false, false, "");
                    myConnectionTester = null;

                    if (modified) {
                        try {
                            myConfigurable.apply();
                            myInnerApplied = true;
                        }
                        catch (ConfigurationException e) {
                            setConnectionStatus(true, false, e.getMessage());
                        }
                    }
                }
                return result;
            }

            @Override
            protected void run() {
                setConnectionStatus(false, false, CloudBundle.message("cloud.status.connecting"));

                myConnectionTester = new ConnectionTester();
                myConnectionTester.testConnection();
            }
        };
        myRunner.queueChangesCheck();
    }

    private static <C extends ServerConfiguration> RemoteServerConfigurable createConfigurable(RemoteServer<C> server, C configuration) {
        return server.getType().createServerConfigurable(configuration);
    }

    private void setConnectionStatus(boolean error, boolean connected, String text) {
        myConnected = connected;
        setConnectionStatusText(error, text);
    }

    protected void setConnectionStatusText(boolean error, String text) {
        myConnectionStatusLabel.setText(UIUtil.toHtml(text));
        myConnectionStatusLabel.setVisible(StringUtil.isNotEmpty(text));
    }

    @Override
    public RemoteServer<?> getEditableObject() {
        return myServer;
    }

    @Override
    public String getBannerSlogan() {
        return myServer.getName();
    }

    @RequiredUIAccess
    @Override
    public JComponent createOptionsPanel(Disposable uiDisposable) {
        mySettingsPanel.add(BorderLayout.CENTER, ConfigurableUIMigrationUtil.createComponent(myConfigurable, uiDisposable));
        return myMainPanel;
    }

    @Override
    public @Nls String getDisplayName() {
        return myServerName;
    }

    @Override
    public @Nullable String getHelpTopic() {
        return myServer.getType().getHelpTopic();
    }

    @Override
    public void setDisplayName(String name) {
        myServerName = name;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return myNew || myConfigurable.isModified() || myInnerApplied || !myServerName.equals(myServer.getName());
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean uncheckedApply = myConfigurable.isModified();
        myConfigurable.apply();
        XmlSerializerUtil.copyBean(myInnerServer.getConfiguration(), myServer.getConfiguration());
        myServer.setName(myServerName);
        myNew = false;
        myAppliedButNeedsCheck = uncheckedApply;
        myInnerApplied = false;
    }

    @Override
    public void reset() {
        myConfigurable.reset();
    }

    @Override
    public void disposeUIResources() {
        myConfigurable.disposeUIResources();
        Disposer.dispose(myRunner);
    }

    @Override
    @Nullable
    public Image getIcon(boolean expanded) {
        return myServer.getType().getIcon();
    }

    private class ConnectionTester {
        private final RemoteServerConnectionTester myTester;

        ConnectionTester() {
            myTester = new RemoteServerConnectionTester(myInnerServer);
        }

        public void testConnection() {
            myTester.testConnection(this::testFinished);
        }

        public void testFinished(boolean connected, @Nonnull String connectionStatus) {
            UIUtil.invokeLaterIfNeeded(() -> {
                if (myConnectionTester == this) {
                    setConnectionStatus(!connected, connected,
                        connected ? CloudBundle.message("cloud.status.connection.successful")
                            : CloudBundle.message("cloud.status.cannot.connect", connectionStatus));
                }
            });
        }
    }
}
