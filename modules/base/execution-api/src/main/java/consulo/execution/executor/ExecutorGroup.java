// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.execution.executor;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * See {@link DefaultExecutorGroup}
 */
public abstract class ExecutorGroup<Settings extends RunExecutorSettings> extends Executor {
    private final ReentrantReadWriteLock myCustomSettingsLock = new ReentrantReadWriteLock();
    private final Map<String, Settings> myExecutorId2CustomSettings = new LinkedHashMap<>();
    private final Map<Settings, ProxyExecutor> myCustomSettings2Executor = new LinkedHashMap<>();
    private final AtomicLong myNextCustomExecutorId = new AtomicLong();

    public abstract LocalizeValue getRunToolbarActionText(String param);

    public abstract LocalizeValue getRunToolbarChooserText();

    protected void registerSettings(Settings settings) {
        myCustomSettingsLock.writeLock().lock();
        try {
            String newId = getId() + "#" + myNextCustomExecutorId.incrementAndGet();
            myExecutorId2CustomSettings.put(newId, settings);
            myCustomSettings2Executor.put(settings, new ProxyExecutor(settings, newId));
        }
        finally {
            myCustomSettingsLock.writeLock().unlock();
        }
    }

    protected void unregisterSettings(Settings settings) {
        myCustomSettingsLock.writeLock().lock();
        try {
            ProxyExecutor executor = myCustomSettings2Executor.remove(settings);
            if (executor != null) {
                myExecutorId2CustomSettings.remove(executor.getId());
            }
        }
        finally {
            myCustomSettingsLock.writeLock().unlock();
        }
    }

    protected List<Pair<String, Settings>> allRegisteredSettings() {
        myCustomSettingsLock.readLock().lock();
        try {
            List<Pair<String, Settings>> result = new ArrayList<>(myExecutorId2CustomSettings.size());
            for (Map.Entry<String, Settings> entry : myExecutorId2CustomSettings.entrySet()) {
                result.add(Pair.create(entry.getKey(), entry.getValue()));
            }
            return result;
        }
        finally {
            myCustomSettingsLock.readLock().unlock();
        }
    }

    /**
     * When any of {@link ExecutorGroup#childExecutors()} started, {@code ProxyExecutor} associated with the selected {@code Settings}
     * instance is used. That {@code ProxyExecutor} is passed through the whole execution system just like any other executor
     * (e.g: {@code consulo.execution.debug.DefaultDebugExecutor}).
     * <p>
     * You can access the selected {@code Settings} by calling {@link ExecutorGroup#getRegisteredSettings(String)} from the appropriate
     * method of your own {@link consulo.execution.configuration.RunConfigurationExtensionBase} implementation.
     */
    public @Nullable Settings getRegisteredSettings(String proxyExecutorId) {
        myCustomSettingsLock.readLock().lock();
        try {
            return myExecutorId2CustomSettings.get(proxyExecutorId);
        }
        finally {
            myCustomSettingsLock.readLock().unlock();
        }
    }

    public List<Executor> childExecutors() {
        myCustomSettingsLock.readLock().lock();
        try {
            return new ArrayList<>(myCustomSettings2Executor.values());
        }
        finally {
            myCustomSettingsLock.readLock().unlock();
        }
    }

    public static @Nullable ExecutorGroup<?> getGroupIfProxy(Executor executor) {
        if (executor instanceof ExecutorGroup<?>.ProxyExecutor proxyExecutor) {
            return proxyExecutor.group();
        }
        return null;
    }

    private class ProxyExecutor extends Executor {
        private final RunExecutorSettings mySettings;
        private final String myExecutorId;

        private ProxyExecutor(RunExecutorSettings settings, String executorId) {
            mySettings = settings;
            myExecutorId = executorId;
        }

        @Override
        public String getToolWindowId() {
            return ExecutorGroup.this.getToolWindowId();
        }

        @Override
        public Image getToolWindowIcon() {
            return ExecutorGroup.this.getToolWindowIcon();
        }

        @Override
        public Image getToolWindowIconIfRunning() {
            return ExecutorGroup.this.getToolWindowIconIfRunning();
        }

        @Override
        public Image getIcon() {
            return mySettings.getIcon();
        }

        @Override
        public @Nullable Image getDisabledIcon() {
            return null;
        }

        @Override
        public LocalizeValue getDescription() {
            return ExecutorGroup.this.getDescription();
        }

        @Override
        public LocalizeValue getActionName() {
            return mySettings.getActionName();
        }

        @Override
        public String getId() {
            return myExecutorId;
        }

        @Override
        public LocalizeValue getStartActionText() {
            return mySettings.getStartActionText();
        }

        @Override
        public LocalizeValue getStartActiveText(String configurationName) {
            return mySettings.getStartActiveText(configurationName);
        }

        @Override
        public @Nullable String getHelpId() {
            return null;
        }

        @Override
        public boolean isApplicable(Project project) {
            return mySettings.isApplicable(project);
        }

        private ExecutorGroup<Settings> group() {
            return ExecutorGroup.this;
        }
    }
}
