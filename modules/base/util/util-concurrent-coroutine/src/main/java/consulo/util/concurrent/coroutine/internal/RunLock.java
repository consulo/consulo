//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine.internal;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A subclass of {@link ReentrantLock} that allows to execute instances of
 * functions like {@link Runnable} or {@link Supplier} inside the typical
 * lock-try-finally-unlock block needed for handling a lock. This is done by
 * calling {@link #runLocked(Runnable)} or {@link #supplyLocked(Supplier)} with
 * the code to execute as the argument.
 *
 * @author eso
 */
public class RunLock extends ReentrantLock {

    private static final long serialVersionUID = 1L;

    /**
     * @see ReentrantLock#ReentrantLock()
     */
    public RunLock() {
    }

    /**
     * @see ReentrantLock#ReentrantLock(boolean)
     */
    public RunLock(boolean fair) {
        super(fair);
    }

    /**
     * Runs the given function inside a lock-try-finally-unlock block with the
     * lock acquired on this instance.
     *
     * @param code The code function to execute
     */
    public void runLocked(Runnable code) {
        lock();

        try {
            code.run();
        }
        finally {
            unlock();
        }
    }

    /**
     * Returns a value that has been supplied by the given function inside a
     * lock-try-finally-unlock block with the lock acquired on this instance.
     *
     * @param code The code function to execute
     * @return The supplied value
     */
    public <T> T supplyLocked(Supplier<T> code) {
        lock();

        try {
            return code.get();
        }
        finally {
            unlock();
        }
    }
}