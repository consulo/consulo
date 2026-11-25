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
package consulo.application.impl.internal;

/**
 * The read lock one thread holds in a {@link StampedRWLock}: the stripe it was taken on and its stamp, zero when the thread
 * holds no read lock.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class StampedReaderState implements RWLock.ReadToken {
    int myStripe;
    long myStamp;

    @Override
    public boolean readRequested() {
        return myStamp != 0;
    }
}
