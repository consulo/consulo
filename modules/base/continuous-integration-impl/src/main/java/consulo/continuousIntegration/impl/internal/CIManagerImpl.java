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
package consulo.continuousIntegration.impl.internal;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.continuousIntegration.CIManager;
import consulo.continuousIntegration.CIManagerListener;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.CIServerOptions;
import consulo.continuousIntegration.CIServerType;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since 2026-11-09
 */
@Singleton
@State(name = "ContinuousIntegrationManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class CIManagerImpl implements CIManager, PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(CIManagerImpl.class);

    private final Project myProject;
    private final AccountManager myAccountManager;
    private final CIServerStore myStore = new CIServerStore();
    private final EventDispatcher<CIManagerListener> myDispatcher = EventDispatcher.create(CIManagerListener.class);
    private final AtomicBoolean myAccountsListened = new AtomicBoolean();

    private volatile List<CIServerEntry> myEntries = List.of();
    private volatile List<CIServer> myServers = List.of();

    @Inject
    public CIManagerImpl(Project project, AccountManager accountManager) {
        myProject = project;
        myAccountManager = accountManager;
    }

    @Override
    public List<CIServer> getServers() {
        return myServers;
    }

    @Override
    public void addListener(CIManagerListener listener, Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    public List<CIServerEntry> getServerEntries() {
        return myEntries;
    }

    public Set<String> getHiddenKeys() {
        return myStore.getHiddenKeys();
    }

    public void setServerEntries(List<CIServerEntry> configured, Set<String> hiddenKeys) {
        myStore.set(configured, hiddenKeys);
        rebuildServers();
    }

    public void projectOpened() {
        if (myAccountsListened.compareAndSet(false, true)) {
            myAccountManager.addListener(new AccountListener() {
                @Override
                public void accountsChanged() {
                    rebuildServers();
                }

                @Override
                public void accountChanged(Account account) {
                    rebuildServers();
                }
            }, this);
        }
        rebuildServers();
    }

    public void rebuildServers() {
        List<CIServerEntry> entries = myStore.merge(detectServers());
        List<CIServer> servers = new ArrayList<>(entries.size());
        for (CIServerEntry entry : entries) {
            try {
                servers.add(entry.createServer(myProject));
            }
            catch (Exception e) {
                LOG.error("Cannot create server " + entry.getType().getId(), e);
            }
        }
        myEntries = List.copyOf(entries);
        myServers = List.copyOf(servers);
        myDispatcher.getMulticaster().serversChanged();
    }

    private List<CIServerEntry> detectServers() {
        List<CIServerEntry> entries = new ArrayList<>();
        myProject.getApplication().getExtensionPoint(CIServerType.class).forEach(rawType -> {
            CIServerType<?> type = rawType;
            AccountType<?> accountType = myAccountManager.findAccountType(type.getAccountTypeId());
            if (accountType == null) {
                return;
            }
            for (Account account : myAccountManager.getAccounts(accountType, myProject)) {
                try {
                    for (CIServerOptions options : type.detectUnchecked(myProject, account)) {
                        if (options.accountId == null) {
                            options.accountId = account.getId();
                        }
                        if (options.serverUrl.isEmpty()) {
                            options.serverUrl = StringUtil.notNullize(myAccountManager.getServerUrl(account));
                        }
                        entries.add(new CIServerEntry(type, options, true));
                    }
                }
                catch (Exception e) {
                    LOG.error("Cannot detect " + type.getId() + " servers for account " + account.getId(), e);
                }
            }
        });
        return entries;
    }

    public @Nullable CIServerType<?> findServerType(String id) {
        return myProject.getApplication().getExtensionPoint(CIServerType.class).findFirstSafe(type -> type.getId().equals(id));
    }

    public @Nullable CIServerEntry findEntry(CIServer server) {
        List<CIServer> servers = myServers;
        int index = servers.indexOf(server);
        List<CIServerEntry> entries = myEntries;
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    @Override
    public Element getState() {
        return myStore.write();
    }

    @Override
    public void loadState(Element state) {
        myStore.read(state, this::findServerType);
        rebuildServers();
    }

    @Override
    public void dispose() {
    }
}
