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

import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class RestrictingAuthenticationGate implements AuthenticationGate {
    private static final Logger LOG = Logger.getInstance(RestrictingAuthenticationGate.class);

    private final Semaphore mySemaphore = new Semaphore(1);
    private volatile boolean myCancelled;
    private final Map<String, String> myInputData = new ConcurrentHashMap<>();

    @Override
    public <T extends @Nullable Object> T waitAndCompute(Supplier<T> operation) {
        try {
            LOG.debug("Entered waitAndCompute");
            mySemaphore.acquire();
            LOG.debug("Acquired permission");
            if (myCancelled) {
                LOG.debug("Authentication Gate has already been cancelled");
                throw new ProcessCanceledException();
            }
            return operation.get();
        }
        catch (InterruptedException e) {
            LOG.warn(e);
            throw new ProcessCanceledException(e);
        }
        finally {
            mySemaphore.release();
        }
    }

    @Override
    public void cancel() {
        myCancelled = true;
    }

    @Override
    public @Nullable String getSavedInput(String key) {
        return myInputData.get(key);
    }

    @Override
    public void saveInput(String key, String value) {
        myInputData.put(key, value);
    }
}
