/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.rawHighlight;

import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HighlightDisplayKey {
    private static final Logger LOG = Logger.getInstance(HighlightDisplayKey.class);

    // TODO [VISTALL] this registry unsafe, and plugins not remove data after uninstall(in runtime)
    private static final Map<String, HighlightDisplayKey> ourNameToKeyMap = new ConcurrentHashMap<>();
    private static final Map<String, HighlightDisplayKey> ourIdToKeyMap = new ConcurrentHashMap<>();
    private static final Map<HighlightDisplayKey, Supplier<String>> ourKeyToDisplayNameMap = new ConcurrentHashMap<>();
    private static final Map<HighlightDisplayKey, String> ourKeyToAlternativeIDMap = new ConcurrentHashMap<>();

    private final String myName;
    private final String myID;

    public static HighlightDisplayKey find(@Nonnull String name) {
        return ourNameToKeyMap.get(name);
    }

    @Nullable
    public static HighlightDisplayKey findById(@Nonnull String id) {
        HighlightDisplayKey key = ourIdToKeyMap.get(id);
        if (key != null) {
            return key;
        }
        key = ourNameToKeyMap.get(id);
        if (key != null && key.getID().equals(id)) {
            return key;
        }
        return null;
    }

    @Nullable
    public static HighlightDisplayKey register(@Nonnull String name) {
        if (find(name) != null) {
            LOG.info("Key with name \'" + name + "\' already registered");
            return null;
        }
        return new HighlightDisplayKey(name);
    }

    /**
     * @see #register(String, Supplier)
     */
    @Nullable
    public static HighlightDisplayKey register(@Nonnull String name, @Nonnull String displayName) {
        return register(name, displayName, name);
    }

    @Nullable
    public static HighlightDisplayKey register(@Nonnull String name, @Nonnull Supplier<String> displayName) {
        return register(name, displayName, name);
    }


    /**
     * @see #register(String, Supplier, String)
     */
    @Nullable
    public static HighlightDisplayKey register(@Nonnull String name, @Nonnull String displayName, @Nonnull String id) {
        return register(name, () -> displayName, id);
    }

    @Nullable
    public static HighlightDisplayKey register(@Nonnull String name, @Nonnull Supplier<String> displayName, @Nonnull String id) {
        if (find(name) != null) {
            LOG.info("Key with name \'" + name + "\' already registered");
            return null;
        }
        HighlightDisplayKey highlightDisplayKey = new HighlightDisplayKey(name, id);
        ourKeyToDisplayNameMap.put(highlightDisplayKey, displayName);
        return highlightDisplayKey;
    }

    @Nullable
    public static HighlightDisplayKey register(
        @Nonnull String name,
        @Nonnull Supplier<String> displayName,
        @Nonnull String id,
        @Nullable String alternativeID
    ) {
        HighlightDisplayKey key = register(name, displayName, id);
        if (alternativeID != null) {
            ourKeyToAlternativeIDMap.put(key, alternativeID);
        }
        return key;
    }

    @Nonnull
    public static HighlightDisplayKey findOrRegister(@Nonnull String name, @Nonnull String displayName) {
        return findOrRegister(name, displayName, null);
    }

    @Nonnull
    public static HighlightDisplayKey findOrRegister(@Nonnull String name, @Nonnull String displayName, @Nullable String id) {
        HighlightDisplayKey key = find(name);
        if (key == null) {
            key = register(name, displayName, id != null ? id : name);
            assert key != null : name;
        }
        return key;
    }

    @Nullable
    public static String getDisplayNameByKey(@Nullable HighlightDisplayKey key) {
        if (key == null) {
            return null;
        }
        else {
            Supplier<String> computable = ourKeyToDisplayNameMap.get(key);
            return computable == null ? null : computable.get();
        }
    }

    public static String getAlternativeID(@Nonnull HighlightDisplayKey key) {
        return ourKeyToAlternativeIDMap.get(key);
    }


    private HighlightDisplayKey(@Nonnull String name) {
        this(name, name);
    }

    public HighlightDisplayKey(@Nonnull String name, @Nonnull String ID) {
        myName = name;
        myID = ID;
        ourNameToKeyMap.put(myName, this);
        if (!Comparing.equal(ID, name)) {
            ourIdToKeyMap.put(ID, this);
        }
    }

    @Override
    public String toString() {
        return myName;
    }

    @Nonnull
    public String getID() {
        return myID;
    }
}
