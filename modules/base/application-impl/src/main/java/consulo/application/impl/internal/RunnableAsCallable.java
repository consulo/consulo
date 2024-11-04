/*
 * Copyright 2013-2020 consulo.io
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
package consulo.application.impl.internal;

import consulo.logging.Logger;
import consulo.util.lang.ControlFlowException;

import java.util.concurrent.Callable;

/**
 * @author VISTALL
 * @since 2020-05-24
 */
public class RunnableAsCallable implements Callable<Void> {
    private final Runnable myRunnable;
    private final Logger myLogger;

    RunnableAsCallable(Runnable runnable, Logger logger) {
        myRunnable = runnable;
        myLogger = logger;
    }

    public Runnable getRunnable() {
        return myRunnable;
    }

    @Override
    public Void call() throws Exception {
        try {
            myRunnable.run();
        }
        catch (Throwable e) {
            if (!(e instanceof ControlFlowException)) {
                myLogger.error(e);
            }
        }
        finally {
            Thread.interrupted(); // reset interrupted status
        }
        return null;
    }

    @Override
    public String toString() {
        return myRunnable.toString();
    }
}
