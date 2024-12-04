// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.disposer.Disposer;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.RuntimeConfigurationWarning;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.impl.internal.configuration.RemoteServerConnectionTester;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class RemoteServerComboWithAutoDetect<S extends ServerConfiguration> extends RemoteServerCombo<S> {
    private AutoDetectedItem myAutoDetectedItem;

    public RemoteServerComboWithAutoDetect(@Nonnull ServerType<S> serverType) {
        super(serverType);
    }

    @Override
    protected @Nonnull ServerItem getNoServersItem() {
        return getServerType().canAutoDetectConfiguration() ? findOrCreateAutoDetectedItem() : super.getNoServersItem();
    }

    protected AutoDetectedItem findOrCreateAutoDetectedItem() {
        if (myAutoDetectedItem == null) {
            myAutoDetectedItem = new AutoDetectedItem();
        }
        return myAutoDetectedItem;
    }

    public void validateAutoDetectedItem() throws RuntimeConfigurationException {
        if (myAutoDetectedItem != null && myAutoDetectedItem == getSelectedItem()) {
            myAutoDetectedItem.validateConnection();
        }
    }

    private enum TestConnectionState {
        INITIAL {
            @Override
            public void validateConnection() throws RuntimeConfigurationException {
                //
            }
        },
        IN_PROGRESS {
            @Override
            public void validateConnection() throws RuntimeConfigurationException {
                throw new RuntimeConfigurationWarning(CloudBundle.message("remote.server.combo.message.test.connection.in.progress"));
            }
        },
        SUCCESSFUL {
            @Override
            public void validateConnection() throws RuntimeConfigurationException {
                //
            }
        },
        FAILED {
            @Override
            public void validateConnection() throws RuntimeConfigurationException {
                throw new RuntimeConfigurationError(
                    CloudBundle.message("remote.server.combo.message.test.connection.failed")/*, () -> createAndEditNewServer()*/);
            }
        };

        public abstract void validateConnection() throws RuntimeConfigurationException;
    }

    private class AutoDetectedItem extends RemoteServerCombo.ServerItemImpl {
        private final AtomicReference<TestConnectionState> myTestConnectionStateA = new AtomicReference<>(TestConnectionState.INITIAL);
        private volatile RemoteServer<S> myServerInstance;
        private volatile long myLastStartedTestConnectionMillis = -1;

        AutoDetectedItem() {
            super(null);
        }

        @Override
        public void render(@Nonnull SimpleColoredComponent ui) {
            ui.setIcon(getServerType().getIcon());

            boolean failed = myTestConnectionStateA.get() == TestConnectionState.FAILED;
            ui.append(CloudBundle.message("remote.server.combo.auto.detected.server", getServerType().getPresentableName()),
                failed ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
        }

        public void validateConnection() throws RuntimeConfigurationException {
            myTestConnectionStateA.get().validateConnection();
        }

        @Override
        public void onBrowseAction() {
            createAndEditNewServer();
        }

        @Override
        public void onItemChosen() {
            if (myServerInstance == null) {
                myServerInstance = RemoteServersManager.getInstance().createServer(getServerType());
                RemoteServerConnectionTester tester = new RemoteServerConnectionTester(myServerInstance);
                setTestConnectionState(TestConnectionState.IN_PROGRESS);
                myLastStartedTestConnectionMillis = System.currentTimeMillis();
                tester.testConnection(this::connectionTested);
            }
        }

        @Override
        public @Nullable String getServerName() {
            return null;
        }

        @Override
        public @Nullable RemoteServer<S> findRemoteServer() {
            return myServerInstance;
        }

        private void setTestConnectionState(@Nonnull TestConnectionState state) {
            boolean changed = myTestConnectionStateA.getAndSet(state) != state;
            if (changed) {
                UIUtil.invokeLaterIfNeeded(RemoteServerComboWithAutoDetect.this::fireStateChanged);
            }
        }

        private void connectionTested(boolean wasConnected, @SuppressWarnings("unused") String errorStatus) {
            assert myLastStartedTestConnectionMillis > 0;
            waitABit(2000);

            if (wasConnected) {
                setTestConnectionState(TestConnectionState.SUCCESSFUL);
                UIUtil.invokeLaterIfNeeded(() -> {
                    if (!Disposer.isDisposed(RemoteServerComboWithAutoDetect.this)) {
                        assert myServerInstance != null;
                        RemoteServersManager.getInstance().addServer(myServerInstance);
                        refillModel(myServerInstance);
                    }
                    myServerInstance = null;
                });
            }
            else {
                setTestConnectionState(TestConnectionState.FAILED);
                myServerInstance = null;
            }
        }

        /**
         * Too quick validation just flickers the screen, so we will ensure that validation message is shown for at least some time
         */
        private void waitABit(@SuppressWarnings("SameParameterValue") long maxTotalDelayMillis) {
            final long THRESHOLD_MS = 50;
            long naturalDelay = System.currentTimeMillis() - myLastStartedTestConnectionMillis;
            if (naturalDelay > 0 && naturalDelay + THRESHOLD_MS < maxTotalDelayMillis) {
                try {
                    Thread.sleep(maxTotalDelayMillis - naturalDelay - THRESHOLD_MS);
                }
                catch (InterruptedException ignored) {
                    //
                }
            }
            myLastStartedTestConnectionMillis = -1;
        }
    }
}
