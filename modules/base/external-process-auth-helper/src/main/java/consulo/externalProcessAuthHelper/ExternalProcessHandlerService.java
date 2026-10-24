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
package consulo.externalProcessAuthHelper;

import consulo.builtinWebServer.BuiltInServerManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import externalApp.ExternalApp;
import externalApp.ExternalAppHandler;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The provider of external application scripts called by Git when a remote operation needs communication with the user.
 * <p>
 * Usage:
 * <ul>
 * <li>Register corresponding {@link ExternalProcessHandlerService} service and {@link ExternalProcessRest} extension.</li>
 * <li>Get the script path from {@link #getCallbackScriptPath} and IDE port from {@link #getIdePort}.</li>
 * <li>Register handler using {@link #registerHandler}.</li>
 * <li>Call external application, passing ENV from {@link ExternalAppHandler} pointing to the script and IDE port.</li>
 * <li>If the operation requests user interaction, the registered handler is called via {@link ExternalApp} and IDE Rest protocol.</li>
 * <li>Do the task (ex: show GUI dialog) and return the answer to the external application via {@link ExternalApp}.</li>
 * </ul>
 */
public abstract class ExternalProcessHandlerService<T extends ExternalAppHandler> {
    private static final Logger LOG = Logger.getInstance(ExternalProcessHandlerService.class);

    private final String myScriptNamePrefix;
    private final Class<? extends ExternalApp> myScriptMainClass;

    private final Map<String, File> myScriptPaths = new HashMap<>();
    private final Object myScriptFileLock = new Object();

    private final Map<UUID, T> myHandlers = new ConcurrentHashMap<>();

    protected ExternalProcessHandlerService(String scriptNamePrefix, Class<? extends ExternalApp> scriptMainClass) {
        myScriptNamePrefix = scriptNamePrefix;
        myScriptMainClass = scriptMainClass;
    }

    public int getIdePort() {
        return BuiltInServerManager.getInstance().waitForStart().getPort();
    }

    /**
     * Get file to the script service
     *
     * @return path to the script
     */
    public File getCallbackScriptPath(String scriptId, ScriptGenerator generator, boolean useBatchFile) throws IOException {
        String id = scriptId + (useBatchFile ? "-bat" : "");

        synchronized (myScriptFileLock) {
            File oldFile = myScriptPaths.get(id);
            if (oldFile != null && oldFile.exists()) {
                return oldFile;
            }

            String commandLine = generator.commandLine(myScriptMainClass, useBatchFile);
            File newFile = ScriptGeneratorUtil.createTempScript(commandLine, myScriptNamePrefix + "-" + scriptId, useBatchFile);
            myScriptPaths.put(id, newFile);
            return newFile;
        }
    }

    public UUID registerHandler(T handler, Disposable disposable) {
        UUID key = UUID.randomUUID();
        myHandlers.put(key, handler);

        Disposer.register(disposable, () -> unregisterHandler(key));

        return key;
    }

    private void unregisterHandler(UUID key) {
        T handler = myHandlers.remove(key);
        if (handler == null) {
            LOG.error("The handler " + key + " is not registered");
        }
    }

    boolean validateHandler(UUID key) {
        return myHandlers.containsKey(key);
    }

    @Nullable String invokeHandler(UUID key, String requestBody) {
        T handler = myHandlers.get(key);
        if (handler == null) {
            throw new IllegalStateException("No handler for the key " + key);
        }
        return handleRequest(handler, requestBody);
    }

    protected abstract @Nullable String handleRequest(T handler, String requestBody);
}
