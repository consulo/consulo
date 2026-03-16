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
package consulo.util.collection.impl.map.base;

import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-03-16
 */
public abstract class ConcurrentRefKeyRefValueMapTestBase extends ConcurrentRefValueMapTestBase {
    @Override
    protected boolean keySetSupported() {
        return false;
    }

    @Override
    protected boolean entrySetSupported() {
        return false;
    }

    @Override
    protected boolean equalsAndHashCodeSupported() {
        return false;
    }

    @Override
    public void testToString() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map).hasToString(map.getClass().getName() + "@" + Integer.toHexString(map.hashCode()));
    }
}
