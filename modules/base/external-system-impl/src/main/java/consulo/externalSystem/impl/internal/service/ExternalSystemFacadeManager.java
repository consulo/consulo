package consulo.externalSystem.impl.internal.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.wrapper.ExternalSystemFacadeWrapper;
import consulo.externalSystem.impl.internal.util.IntegrationKey;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Entry point to work with remote {@link RemoteExternalSystemFacade}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/8/11 1:08 PM
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ExternalSystemFacadeManager {

    private static final int REMOTE_FAIL_RECOVERY_ATTEMPTS_NUMBER = 3;

    private final ConcurrentMap<IntegrationKey, RemoteExternalSystemFacade> myFacadeWrappers = ContainerUtil.newConcurrentMap();

    private final Map<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> myRemoteFacades
        = ContainerUtil.newConcurrentMap();

    
    private final Lock myLock = new ReentrantLock();

    
    private final RemoteExternalSystemProgressNotificationManager myProgressManager;

    
    private final InProcessExternalSystemCommunicationManager myInProcessCommunicationManager;

    @Inject
    public ExternalSystemFacadeManager(ExternalSystemProgressNotificationManager notificationManager,
                                       InProcessExternalSystemCommunicationManager inProcessCommunicationManager) {
        myProgressManager = (RemoteExternalSystemProgressNotificationManager) notificationManager;
        myInProcessCommunicationManager = inProcessCommunicationManager;
    }

    
    private static Project findProject(IntegrationKey key) {
        ProjectManager projectManager = ProjectManager.getInstance();
        for (Project project : projectManager.getOpenProjects()) {
            if (key.getIdeProjectName().equals(project.getName()) && key.getIdeProjectLocationHash().equals(project.getLocationHash())) {
                return project;
            }
        }
        return projectManager.getDefaultProject();
    }

    public void onProjectRename(String oldName, String newName) {
        onProjectRename(myFacadeWrappers, oldName, newName);
        onProjectRename(myRemoteFacades, oldName, newName);
    }

    private static <V> void onProjectRename(Map<IntegrationKey, V> data,
                                            String oldName,
                                            String newName) {
        Set<IntegrationKey> keys = new HashSet<>(data.keySet());
        for (IntegrationKey key : keys) {
            if (!key.getIdeProjectName().equals(oldName)) {
                continue;
            }
            IntegrationKey newKey = new IntegrationKey(newName,
                key.getIdeProjectLocationHash(),
                key.getExternalSystemId(),
                key.getExternalProjectConfigPath());
            V value = data.get(key);
            data.put(newKey, value);
            data.remove(key);
            if (value instanceof Consumer) {
                //noinspection unchecked
                ((Consumer) value).accept(newKey);
            }
        }
    }

    /**
     * @return gradle api facade to use
     * @throws Exception in case of inability to return the facade
     */
    public RemoteExternalSystemFacade getFacade(@Nullable Project project,
                                                String externalProjectPath,
                                                ProjectSystemId externalSystemId) throws Exception {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        IntegrationKey key = new IntegrationKey(project, externalSystemId, externalProjectPath);
        RemoteExternalSystemFacade facade = myFacadeWrappers.get(key);
        if (facade == null) {
            RemoteExternalSystemFacade newFacade = (RemoteExternalSystemFacade) Proxy.newProxyInstance(
                ExternalSystemFacadeManager.class.getClassLoader(), new Class[]{RemoteExternalSystemFacade.class, Consumer.class},
                new MyHandler(key)
            );
            myFacadeWrappers.putIfAbsent(key, newFacade);
        }
        return myFacadeWrappers.get(key);
    }

    public Object doInvoke(IntegrationKey key, Project project, Method method, Object[] args, int invocationNumber)
        throws Throwable {
        RemoteExternalSystemFacade facade = doGetFacade(key, project);
        try {
            return method.invoke(facade, args);
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ExternalSystemCommunicationException && invocationNumber > 0) {
                Thread.sleep(1000);
                return doInvoke(key, project, method, args, invocationNumber - 1);
            }
            else {
                throw e;
            }
        }
    }

    public ExternalSystemCommunicationManager getCommunicationManager() {
        return myInProcessCommunicationManager;
    }

    public ExternalSystemCommunicationManager getCommunicationManager(ProjectSystemId externalSystemId) {
        return myInProcessCommunicationManager;
    }

    @SuppressWarnings("ConstantConditions")
    
    private RemoteExternalSystemFacade doGetFacade(IntegrationKey key, Project project) throws Exception {
        ExternalSystemCommunicationManager myCommunicationManager = myInProcessCommunicationManager;

        ExternalSystemManager manager = ExternalSystemApiUtil.getManager(key.getExternalSystemId());
        if (project.isDisposed() || manager == null) {
            return RemoteExternalSystemFacade.NULL_OBJECT;
        }
        Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> pair = myRemoteFacades.get(key);
        if (pair != null && prepare(myCommunicationManager, project, key, pair)) {
            return pair.first;
        }

        myLock.lock();
        try {
            pair = myRemoteFacades.get(key);
            if (pair != null && prepare(myCommunicationManager, project, key, pair)) {
                return pair.first;
            }
            if (pair != null) {
                myFacadeWrappers.clear();
                myRemoteFacades.clear();
            }
            return doCreateFacade(key, project, myCommunicationManager);
        }
        finally {
            myLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    
    private RemoteExternalSystemFacade doCreateFacade(IntegrationKey key, Project project,
                                                      ExternalSystemCommunicationManager communicationManager) throws Exception {
        RemoteExternalSystemFacade facade = communicationManager.acquire(project.getName(), key.getExternalSystemId());
        if (facade == null) {
            throw new IllegalStateException("Can't obtain facade to working with external api at the remote process. Project: " + project);
        }
        Disposer.register(project, new Disposable() {
            @Override
            public void dispose() {
                myFacadeWrappers.clear();
                myRemoteFacades.clear();
            }
        });
        RemoteExternalSystemFacade result = new ExternalSystemFacadeWrapper(facade, myProgressManager);
        ExternalSystemExecutionSettings settings
            = ExternalSystemApiUtil.getExecutionSettings(project, key.getExternalProjectConfigPath(), key.getExternalSystemId());
        Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> newPair = Pair.create(result, settings);
        myRemoteFacades.put(key, newPair);
        result.applySettings(newPair.second);
        return result;
    }

    @SuppressWarnings("unchecked")
    private boolean prepare(ExternalSystemCommunicationManager communicationManager,
                            Project project, IntegrationKey key,
                            Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> pair) {
        if (!communicationManager.isAlive(pair.first)) {
            return false;
        }
        try {
            ExternalSystemExecutionSettings currentSettings
                = ExternalSystemApiUtil.getExecutionSettings(project, key.getExternalProjectConfigPath(), key.getExternalSystemId());
            if (!currentSettings.equals(pair.second)) {
                pair.first.applySettings(currentSettings);
                myRemoteFacades.put(key, Pair.create(pair.first, currentSettings));
            }
            return true;
        }
        catch (ExternalSystemCommunicationException e) {
            return false;
        }
    }

    public boolean isTaskActive(ExternalSystemTaskId id) {
        Map<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> copy
            = new HashMap<>(myRemoteFacades);
        for (Map.Entry<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> entry : copy.entrySet()) {
            try {
                if (entry.getValue().first.isTaskInProgress(id)) {
                    return true;
                }
            }
            catch (ExternalSystemCommunicationException e) {
                myLock.lock();
                try {
                    myRemoteFacades.remove(entry.getKey());
                    myFacadeWrappers.remove(entry.getKey());
                }
                finally {
                    myLock.unlock();
                }
            }
        }
        return false;
    }

    private class MyHandler implements InvocationHandler {

        
        private final AtomicReference<IntegrationKey> myKey = new AtomicReference<IntegrationKey>();

        MyHandler(IntegrationKey key) {
            myKey.set(key);
        }

        @Override
        public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("consume".equals(method.getName())) {
                myKey.set((IntegrationKey) args[0]);
                return null;
            }
            Project project = findProject(myKey.get());
            return doInvoke(myKey.get(), project, method, args, REMOTE_FAIL_RECOVERY_ATTEMPTS_NUMBER);
        }
    }
}
