// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.impl.internal.runtime.deployment.DeploymentImpl;
import consulo.remoteServer.impl.internal.runtime.deployment.DeploymentTaskImpl;
import consulo.remoteServer.impl.internal.runtime.deployment.LocalDeploymentImpl;
import consulo.remoteServer.impl.internal.runtime.log.DeploymentLogManagerImpl;
import consulo.remoteServer.impl.internal.runtime.log.LoggingHandlerImpl;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.deployment.*;
import consulo.remoteServer.runtime.deployment.debug.DebugConnectionData;
import consulo.remoteServer.runtime.deployment.debug.DebugConnectionDataNotAvailableException;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import consulo.util.lang.EmptyRunnable;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServerConnectionImpl<D extends DeploymentConfiguration> implements ServerConnection<D> {
    private static final Logger LOG = Logger.getInstance(ServerConnectionImpl.class);
    private final RemoteServer<?> myServer;
    private final ServerConnector<D> myConnector;
    private final ServerConnectionEventDispatcher myEventDispatcher;
    private final ServerConnectionManagerImpl myConnectionManager;
    private MessageBusConnection myMessageBusConnection;

    private volatile ConnectionStatus myStatus = ConnectionStatus.DISCONNECTED;
    private volatile LocalizeValue myStatusText = LocalizeValue.empty();
    private volatile ServerRuntimeInstance<D> myRuntimeInstance;
    private final Map<Project, LogManagersForProject> myPerProjectLogManagers = new ConcurrentHashMap<>();
    private final MyDeployments myAllDeployments;

    public ServerConnectionImpl(RemoteServer<?> server,
                                ServerConnector<D> connector,
                                @Nullable ServerConnectionManagerImpl connectionManager,
                                ServerConnectionEventDispatcher eventDispatcher) {
        myServer = server;
        myConnector = connector;
        myConnectionManager = connectionManager;
        myEventDispatcher = eventDispatcher;
        myAllDeployments = new MyDeployments(server.getType().getDeploymentComparator());
    }

    @Override
    public RemoteServer<?> getServer() {
        return myServer;
    }

    @Override
    public ConnectionStatus getStatus() {
        return myStatus;
    }

    @Override
    public LocalizeValue getStatusText() {
        return myStatusText.orIfEmpty(myStatus.getPresentableText());
    }

    @Override
    public void connect(final Runnable onFinished) {
        doDisconnect();
        connectIfNeeded(new ServerConnector.ConnectionCallback<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> serverRuntimeInstance) {
                onFinished.run();
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                onFinished.run();
            }
        });
    }

    @Override
    public void disconnect() {
        if (myConnectionManager != null) {
            myConnectionManager.removeConnection(myServer);
        }
        doDisconnect();
    }

    private void doDisconnect() {
        if (myStatus == ConnectionStatus.CONNECTED) {
            if (myRuntimeInstance != null) {
                myRuntimeInstance.disconnect();
                myRuntimeInstance = null;
            }
            setStatus(ConnectionStatus.DISCONNECTED);
            for (LogManagersForProject forNextProject : myPerProjectLogManagers.values()) {
                forNextProject.disposeAllLogs();
            }
            if (myMessageBusConnection != null) {
                myMessageBusConnection.disconnect();
                myMessageBusConnection = null;
            }
        }
    }

    @Override
    public void deploy(final DeploymentTask<D> task, final Consumer<? super String> onDeploymentStarted) {
        connectIfNeeded(new ConnectionCallbackBase<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> instance) {
                LocalDeploymentImpl<?> deployment = new LocalDeploymentImpl<>(instance,
                    ServerConnectionImpl.this,
                    DeploymentStatus.DEPLOYING,
                    null,
                    null,
                    task);
                String deploymentName = deployment.getName();
                myAllDeployments.addLocal(deployment);

                DeploymentLogManagerImpl logManager = myPerProjectLogManagers.computeIfAbsent(task.getProject(), LogManagersForProject::new)
                    .findOrCreateManager(deployment)
                    .withMainHandlerVisible(true);

                LoggingHandlerImpl handler = logManager.getMainLoggingHandler();
                handler.printlnSystemMessage("Deploying '" + deploymentName + "'...");
                onDeploymentStarted.accept(deploymentName);
                instance
                    .deploy(task, logManager, new DeploymentOperationCallbackImpl(deploymentName, (DeploymentTaskImpl<D>) task, handler, deployment));
            }
        });
    }

    @Override
    public @Nullable DeploymentLogManager getLogManager(Project project, Deployment deployment) {
        LogManagersForProject forProject = myPerProjectLogManagers.get(project);
        return forProject == null ? null : forProject.findManager(deployment);
    }

    public DeploymentLogManager getOrCreateLogManager(Project project, Deployment deployment) {
        LogManagersForProject forProject = myPerProjectLogManagers.computeIfAbsent(project, LogManagersForProject::new);
        return forProject.findOrCreateManager(deployment);
    }

    @Override
    public void computeDeployments(final Runnable onFinished) {
        connectIfNeeded(new ConnectionCallbackBase<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> instance) {
                computeDeployments(instance, onFinished);
            }
        });
    }

    private void computeDeployments(ServerRuntimeInstance<D> instance, final Runnable onFinished) {
        instance.computeDeployments(new ServerRuntimeInstance.ComputeDeploymentsCallback() {
            private final List<DeploymentImpl<?>> myCollectedDeployments = Collections.synchronizedList(new ArrayList<>());

            @Override
            public void addDeployment(String deploymentName) {
                addDeployment(deploymentName, null);
            }

            @Override
            public void addDeployment(String deploymentName, @Nullable DeploymentRuntime deploymentRuntime) {
                addDeployment(deploymentName, deploymentRuntime, null, LocalizeValue.empty());
            }

            @Override
            public Deployment addDeployment(String name,
                                            @Nullable DeploymentRuntime runtime,
                                            @Nullable DeploymentStatus status,
                                            LocalizeValue statusText) {
                if (status == null) {
                    status = DeploymentStatus.DEPLOYED;
                }
                DeploymentImpl<?> result = myAllDeployments.updateRemoteState(name, runtime, status, statusText);
                if (result == null) {
                    result = new DeploymentImpl<>(ServerConnectionImpl.this,
                        name,
                        status,
                        statusText,
                        runtime,
                        null);
                }
                myCollectedDeployments.add(result);
                return result;
            }

            @Override
            public void succeeded() {
                synchronized (myCollectedDeployments) {
                    myAllDeployments.replaceRemotesWith(myCollectedDeployments);
                }

                myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
                onFinished.run();
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                myAllDeployments.replaceRemotesWith(Collections.emptyList());

                myStatusText = RemoteServerLocalize.serverconnectionimplErrorCannotObtainDeployments(errorMessage);
                myEventDispatcher.queueConnectionStatusChanged(ServerConnectionImpl.this);
                myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
                onFinished.run();
            }
        });
    }

    @Override
    public void undeploy(Deployment deployment, @Nullable DeploymentRuntime runtime) {
        String deploymentName = deployment.getName();
        final MyDeployments.UndeployTransition undeployInProgress = myAllDeployments.startUndeploy(deploymentName);

        myEventDispatcher.queueDeploymentsChanged(this);

        List<LoggingHandlerImpl> handlers = myPerProjectLogManagers.values().stream()
            .map(nextForProject -> nextForProject.findManager(deployment))
            .filter(Objects::nonNull)
            .map(DeploymentLogManagerImpl::getMainLoggingHandler)
            .toList();

        final Consumer<String> logConsumer = message -> {
            if (handlers.isEmpty()) {
                LOG.info(message);
            }
            else {
                handlers.forEach(h -> h.printlnSystemMessage(message));
            }
        };

        logConsumer.accept("Undeploying '" + deploymentName + "'...");

        DeploymentRuntime.UndeploymentTaskCallback undeploymentTaskCallback = new DeploymentRuntime.UndeploymentTaskCallback() {
            @Override
            public void succeeded() {
                logConsumer.accept("'" + deploymentName + "' has been undeployed successfully.");

                Set<String> namesToDispose = new LinkedHashSet<>();
                namesToDispose.add(deploymentName);

                if (undeployInProgress != null) {
                    undeployInProgress.succeeded();
                    undeployInProgress.getSubDeployments().forEach(deployment -> namesToDispose.add(deployment.getName()));
                }

                namesToDispose.forEach(name -> disposeAllLogs(name));

                myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
                computeDeployments(myRuntimeInstance, EmptyRunnable.INSTANCE);
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                logConsumer.accept("Failed to undeploy '" + deploymentName + "': " + errorMessage);

                if (undeployInProgress != null) {
                    undeployInProgress.failed();
                }

                myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
            }
        };

        if (runtime == null) {
            undeploymentTaskCallback.succeeded();
        }
        else {
            runtime.undeploy(undeploymentTaskCallback);
        }
    }

    public void disposeAllLogs(DeploymentImpl deployment) {
        disposeAllLogs(deployment.getName());
    }

    public void disposeAllLogs(String deploymentName) {
        myPerProjectLogManagers.values().forEach(nextForProject -> nextForProject.disposeManager(deploymentName));
    }

    @Override
    public Collection<Deployment> getDeployments() {
        return myAllDeployments.listDeployments();
    }

    private void setupProjectListener() {
        if (myMessageBusConnection == null) {
            myMessageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
            myMessageBusConnection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
                @Override
                public void projectClosed(Project project) {
                    onProjectClosed(project);
                }
            });
        }
    }

    @Override
    public void connectIfNeeded(final ServerConnector.ConnectionCallback<D> callback) {
        ServerRuntimeInstance<D> instance = myRuntimeInstance;
        if (instance != null) {
            callback.connected(instance);
            return;
        }

        setStatus(ConnectionStatus.CONNECTING);
        myConnector.connect(new ServerConnector.ConnectionCallback<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> instance) {
                setStatus(ConnectionStatus.CONNECTED);
                myRuntimeInstance = instance;
                setupProjectListener();
                callback.connected(instance);
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                setStatus(ConnectionStatus.DISCONNECTED, errorMessage);
                myRuntimeInstance = null;
                callback.errorOccurred(errorMessage);
            }
        });
    }

    private void setStatus(ConnectionStatus status) {
        setStatus(status, LocalizeValue.empty());
    }

    private void setStatus(ConnectionStatus status, LocalizeValue statusText) {
        myStatus = status;
        myStatusText = statusText;
        myEventDispatcher.queueConnectionStatusChanged(this);
    }

    public void changeDeploymentState(DeploymentImpl deployment,
                                      @Nullable DeploymentRuntime deploymentRuntime,
                                      DeploymentStatus oldStatus, DeploymentStatus newStatus,
                                      LocalizeValue statusText) {

        if (myAllDeployments.updateAnyState(deployment, deploymentRuntime, oldStatus, newStatus, statusText)) {
            myEventDispatcher.queueDeploymentsChanged(this);
        }
    }

    private void onProjectClosed(Project project) {
        myPerProjectLogManagers.remove(project);
        boolean hasChanged = myAllDeployments.removeAllLocalForProject(project);
        if (hasChanged) {
            myEventDispatcher.queueDeploymentsChanged(this);
        }
    }

    private class LogManagersForProject {
        private final Project myProject;
        private final Map<String, DeploymentLogManagerImpl> myLogManagers = new ConcurrentHashMap<>();

        LogManagersForProject(Project project) {
            myProject = project;
        }

        public @Nullable DeploymentLogManagerImpl findManager(Deployment deployment) {
            return myLogManagers.get(deployment.getName());
        }

        public DeploymentLogManagerImpl findOrCreateManager(Deployment deployment) {
            return myLogManagers.computeIfAbsent(deployment.getName(), this::newDeploymentLogManager);
        }

        private DeploymentLogManagerImpl newDeploymentLogManager(String deploymentName) {
            return new DeploymentLogManagerImpl(myProject, new ChangeListener());
        }

        public void disposeManager(String deploymentName) {
            DeploymentLogManagerImpl manager = myLogManagers.remove(deploymentName);
            if (manager != null) {
                manager.disposeLogs();
            }
        }

        public void disposeAllLogs() {
            for (DeploymentLogManagerImpl nextManager : myLogManagers.values()) {
                nextManager.disposeLogs();
            }
        }
    }

    private abstract static class ConnectionCallbackBase<D extends DeploymentConfiguration> implements ServerConnector.ConnectionCallback<D> {
        @Override
        public void errorOccurred(LocalizeValue errorMessage) {
        }
    }

    private class DeploymentOperationCallbackImpl implements ServerRuntimeInstance.DeploymentOperationCallback {
        private final String myDeploymentName;
        private final DeploymentTaskImpl<D> myDeploymentTask;
        private final LoggingHandlerImpl myLoggingHandler;
        private final DeploymentImpl<?> myDeployment;

        DeploymentOperationCallbackImpl(String deploymentName,
                                        DeploymentTaskImpl<D> deploymentTask,
                                        LoggingHandlerImpl handler,
                                        DeploymentImpl<?> deployment) {
            myDeploymentName = deploymentName;
            myDeploymentTask = deploymentTask;
            myLoggingHandler = handler;
            myDeployment = deployment;
        }

        @Override
        public void started(DeploymentRuntime deploymentRuntime) {
            myDeployment.changeState(myDeployment.getStatus(), DeploymentStatus.DEPLOYING, null, deploymentRuntime);
        }

        @Override
        public Deployment succeeded(DeploymentRuntime deploymentRuntime) {
            myLoggingHandler.printlnSystemMessage("'" + myDeploymentName + "' has been deployed successfully.");
            myDeployment.changeState(DeploymentStatus.DEPLOYING, DeploymentStatus.DEPLOYED, null, deploymentRuntime);
            myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
            DebugConnector<?, ?> debugConnector = myDeploymentTask.getDebugConnector();
            if (debugConnector != null) {
                launchDebugger(debugConnector, deploymentRuntime);
            }
            return myDeployment;
        }

        private <D extends DebugConnectionData, R extends DeploymentRuntime> void launchDebugger(DebugConnector<D, R> debugConnector,
                                                                                                 DeploymentRuntime runtime) {
            try {
                D debugInfo = debugConnector.getConnectionData((R) runtime);
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        debugConnector.getLauncher().startDebugSession(debugInfo, myDeploymentTask.getExecutionEnvironment(), myServer);
                    }
                    catch (ExecutionException e) {
                        myLoggingHandler.print("Cannot start debugger: " + e.getMessage() + "\n");
                        LOG.info(e);
                    }
                });
            }
            catch (DebugConnectionDataNotAvailableException e) {
                myLoggingHandler.print("Cannot retrieve debug connection: " + e.getMessage() + "\n");
                LOG.info(e);
            }
        }

        @Override
        public void errorOccurred(LocalizeValue errorMessage) {
            myLoggingHandler.printlnSystemMessage("Failed to deploy '" + myDeploymentName + "': " + errorMessage);
            myAllDeployments.updateAnyState(myDeployment, null,
                DeploymentStatus.DEPLOYING, DeploymentStatus.NOT_DEPLOYED, errorMessage);
            myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
        }

        @Override
        public void errorOccurred(LocalizeValue errorMessage,
                                  DeploymentRuntime failedDeployment) {
            myLoggingHandler.printlnSystemMessage("Failed to deploy '" + myDeploymentName + "': " + errorMessage.get());
            myDeployment.changeState(DeploymentStatus.DEPLOYING, DeploymentStatus.NOT_DEPLOYED, errorMessage, failedDeployment);
            myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
        }
    }

    private class ChangeListener implements Runnable {

        @Override
        public void run() {
            myEventDispatcher.queueDeploymentsChanged(ServerConnectionImpl.this);
        }
    }

    private static class MyDeployments {
        private final Object myLock = new Object();

        private final Map<String, DeploymentImpl<?>> myRemoteDeployments = new HashMap<>();
        private final Map<String, LocalDeploymentImpl<?>> myLocalDeployments = new HashMap<>();
        private List<Deployment> myCachedAllDeployments;
        private final Comparator<? super Deployment> myDeploymentComparator;

        MyDeployments(Comparator<? super Deployment> deploymentComparator) {
            myDeploymentComparator = deploymentComparator;
        }

        public void addLocal(LocalDeploymentImpl<?> deployment) {
            synchronized (myLock) {
                myLocalDeployments.put(deployment.getName(), deployment);
                myCachedAllDeployments = null;
            }
        }

        public void replaceRemotesWith(Collection<? extends DeploymentImpl> newDeployments) {
            synchronized (myLock) {
                myRemoteDeployments.clear();
                myCachedAllDeployments = null;
                for (DeploymentImpl<?> deployment : newDeployments) {
                    myRemoteDeployments.put(deployment.getName(), deployment);
                }
            }
        }

        public @Nullable DeploymentImpl updateRemoteState(String deploymentName,
                                                          @Nullable DeploymentRuntime deploymentRuntime,
                                                          DeploymentStatus deploymentStatus,
                                                          LocalizeValue deploymentStatusText) {

            synchronized (myLock) {
                DeploymentImpl<?> result = myRemoteDeployments.get(deploymentName);
                if (result != null && !result.getStatus().isTransition()) {
                    result.changeState(result.getStatus(), deploymentStatus, deploymentStatusText, deploymentRuntime);
                }
                return result;
            }
        }

        public boolean updateAnyState(DeploymentImpl deployment,
                                      @Nullable DeploymentRuntime deploymentRuntime,
                                      DeploymentStatus oldStatus,
                                      DeploymentStatus newStatus,
                                      LocalizeValue statusText) {

            synchronized (myLock) {
                return deployment.changeState(oldStatus, newStatus, statusText, deploymentRuntime);
            }
        }

        public Collection<Deployment> listDeployments() {
            synchronized (myLock) {
                if (myCachedAllDeployments == null) {
                    Collection<Deployment> result = doListDeployments();
                    myCachedAllDeployments = List.copyOf(result);
                }
                return myCachedAllDeployments;
            }
        }

        private Collection<Deployment> doListDeployments() {
            //assumed myLock
            Map<Deployment, DeploymentImpl<?>> orderedDeployments = new TreeMap<>(myDeploymentComparator);
            List<LocalDeploymentImpl<?>> matchedLocalsBefore = new ArrayList<>();

            for (LocalDeploymentImpl<?> localDeployment : myLocalDeployments.values()) {
                if (localDeployment.hasRemoteDeloyment()) {
                    matchedLocalsBefore.add(localDeployment);
                }
                localDeployment.setRemoteDeployment(null);
                orderedDeployments.put(localDeployment, localDeployment);
            }

            Set<Deployment> result = new LinkedHashSet<>(orderedDeployments.keySet());

            for (DeploymentImpl<?> remoteDeployment : myRemoteDeployments.values()) {
                DeploymentImpl<?> deployment = orderedDeployments.get(remoteDeployment);
                if (deployment != null) {
                    if (deployment instanceof LocalDeploymentImpl) {
                        ((LocalDeploymentImpl<?>) deployment).setRemoteDeployment(remoteDeployment);
                    }
                }
                else {
                    orderedDeployments.put(remoteDeployment, remoteDeployment);
                }
            }

            DeploymentStatus finishedExternally = DeploymentStatus.NOT_DEPLOYED;
            for (LocalDeploymentImpl<?> nextLocal : matchedLocalsBefore) {
                if (!nextLocal.hasRemoteDeloyment()) {
                    nextLocal.changeState(nextLocal.getStatus(), finishedExternally, LocalizeValue.empty(), null);
                }
            }

            result.addAll(orderedDeployments.keySet());
            return result;
        }

        public boolean removeAllLocalForProject(Project project) {
            synchronized (myLock) {
                boolean hasChanged = false;
                for (Iterator<LocalDeploymentImpl<?>> it = myLocalDeployments.values().iterator(); it.hasNext(); ) {
                    LocalDeploymentImpl<?> nextLocal = it.next();
                    if (nextLocal.getDeploymentTask().getProject() == project) {
                        it.remove();
                        hasChanged = true;
                    }
                }
                if (hasChanged) {
                    myCachedAllDeployments = null;
                }
                return hasChanged;
            }
        }

        public @Nullable UndeployTransition startUndeploy(String deploymentName) {
            synchronized (myLock) {
                DeploymentImpl<?> deployment = myLocalDeployments.get(deploymentName);
                if (deployment == null) {
                    deployment = myRemoteDeployments.get(deploymentName);
                }
                return deployment == null ? null : new UndeployTransition(deployment, collectDeepChildren(deployment));
            }
        }

        private List<Deployment> collectDeepChildren(Deployment root) {
            DeepChildrenCollector collector = new DeepChildrenCollector(root.getRuntime());
            synchronized (myLock) {
                for (LocalDeploymentImpl<?> nextLocal : myLocalDeployments.values()) {
                    collector.visitDeployment(nextLocal);
                }
                for (DeploymentImpl<?> nextRemote : myRemoteDeployments.values()) {
                    collector.visitDeployment(nextRemote);
                }
            }
            return collector.getChildDeployments();
        }

        private class UndeployTransition {
            private final DeploymentImpl<?> myDeployment;
            private final List<Deployment> mySubDeployments;

            UndeployTransition(DeploymentImpl<?> deployment, List<Deployment> subDeployments) {
                myDeployment = deployment;
                mySubDeployments = new ArrayList<>(subDeployments);

                myDeployment.changeState(DeploymentStatus.DEPLOYED, DeploymentStatus.DEPLOYING, null, deployment.getRuntime());
            }

            public void succeeded() {
                synchronized (myLock) {
                    if (tryChangeToTerminalState(DeploymentStatus.NOT_DEPLOYED, true) || myDeployment.getRuntime() == null) {
                        forgetDeployment(myDeployment);

                        for (Deployment nextImplicitlyUndeployed : mySubDeployments) {
                            if (nextImplicitlyUndeployed != myDeployment) {
                                forgetDeployment(nextImplicitlyUndeployed);
                            }
                        }

                        myCachedAllDeployments = null;
                    }
                }
            }

            public void failed() {
                synchronized (myLock) {
                    tryChangeToTerminalState(DeploymentStatus.DEPLOYED, false);
                }
            }

            public Iterable<Deployment> getSubDeployments() {
                return mySubDeployments;
            }

            private boolean tryChangeToTerminalState(DeploymentStatus terminalState, boolean forgetRuntime) {
                //assumed myLock
                DeploymentRuntime targetRuntime = forgetRuntime ? null : myDeployment.getRuntime();
                return myDeployment.changeState(DeploymentStatus.DEPLOYING, terminalState, null, targetRuntime);
            }

            private void forgetDeployment(Deployment deployment) {
                synchronized (myLock) {
                    String deploymentName = deployment.getName();
                    myLocalDeployments.remove(deploymentName);
                    myRemoteDeployments.remove(deploymentName);
                }
            }
        }

        private static class DeepChildrenCollector {
            private final Map<DeploymentRuntime, Boolean> mySettledStatuses = new IdentityHashMap<>();
            private final List<Deployment> myCollectedChildren = new LinkedList<>();
            private final DeploymentRuntime myRootRuntime;

            DeepChildrenCollector(DeploymentRuntime rootRuntime) {
                myRootRuntime = rootRuntime;
            }

            public void visitDeployment(Deployment deployment) {
                if (isUnderRootRuntime(deployment.getRuntime())) {
                    myCollectedChildren.add(deployment);
                }
            }

            private boolean isUnderRootRuntime(@Nullable DeploymentRuntime runtime) {
                if (runtime == null) {
                    return false;
                }
                if (runtime == myRootRuntime) {
                    return true;
                }
                return mySettledStatuses.computeIfAbsent(runtime, rt -> this.isUnderRootRuntime(rt.getParent()));
            }

            public List<Deployment> getChildDeployments() {
                return Collections.unmodifiableList(myCollectedChildren);
            }
        }
    }
}
