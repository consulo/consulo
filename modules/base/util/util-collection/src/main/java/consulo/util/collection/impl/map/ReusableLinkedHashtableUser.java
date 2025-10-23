/*
 * Copyright 2013-2024 consulo.io
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
package consulo.util.collection.impl.map;

/**
 * <p>User of {@link ReusableLinkedHashtable}. Instance implementing this interface must answer if it is a master map
 * and be able to detach from the hash-table via creating a new one containing only the key/value entries owned by the map.</p>
 *
 * @see ReusableLinkedHashtable
 * @author UNV
 * @since 2024-11-20
 */
public interface ReusableLinkedHashtableUser {
    /**
     * <p>Stop reusing current hash-table by creating a new one containing only the key/value entries owned by the map.
     * This prevents memory leak.</p>
     */
    void detachFromTable();
}
