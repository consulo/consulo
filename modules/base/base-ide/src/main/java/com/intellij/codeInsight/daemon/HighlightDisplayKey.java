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
package com.intellij.codeInsight.daemon;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HighlightDisplayKey {
  private static final Logger LOG = Logger.getInstance(HighlightDisplayKey.class);

  private static final Map<String, HighlightDisplayKey> ourNameToKeyMap = new HashMap<>();
  private static final Map<String, HighlightDisplayKey> ourIdToKeyMap = new HashMap<>();
  private static final Map<HighlightDisplayKey, Computable<String>> ourKeyToDisplayNameMap = new HashMap<>();
  private static final Map<HighlightDisplayKey, String> ourKeyToAlternativeIDMap = new HashMap<>();

  private final String myName;
  private final String myID;

  public static HighlightDisplayKey find(@Nonnull final String name) {
    return ourNameToKeyMap.get(name);
  }

  @Nullable
  public static HighlightDisplayKey findById(@Nonnull final String id) {
    HighlightDisplayKey key = ourIdToKeyMap.get(id);
    if (key != null) return key;
    key = ourNameToKeyMap.get(id);
    if (key != null && key.getID().equals(id)) return key;
    return null;
  }

  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name) {
    if (find(name) != null) {
      LOG.info("Key with name \'" + name + "\' already registered");
      return null;
    }
    return new HighlightDisplayKey(name);
  }

  /**
   * @see #register(String, Computable)
   */
  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name, @Nonnull final String displayName) {
    return register(name, displayName, name);
  }

  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name, @Nonnull Computable<String> displayName) {
    return register(name, displayName, name);
  }


  /**
   * @see #register(String, Computable, String)
   */
  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name, @Nonnull final String displayName, @Nonnull final String id) {
    return register(name, new Computable.PredefinedValueComputable<>(displayName), id);
  }

  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name, @Nonnull final Computable<String> displayName, @Nonnull final String id) {
    if (find(name) != null) {
      LOG.info("Key with name \'" + name + "\' already registered");
      return null;
    }
    HighlightDisplayKey highlightDisplayKey = new HighlightDisplayKey(name, id);
    ourKeyToDisplayNameMap.put(highlightDisplayKey, displayName);
    return highlightDisplayKey;
  }

  @Nullable
  public static HighlightDisplayKey register(@Nonnull final String name, @Nonnull final Computable<String> displayName, @Nonnull final String id, @Nullable final String alternativeID) {
    final HighlightDisplayKey key = register(name, displayName, id);
    if (alternativeID != null) {
      ourKeyToAlternativeIDMap.put(key, alternativeID);
    }
    return key;
  }

  @Nonnull
  public static HighlightDisplayKey findOrRegister(@Nonnull String name, @Nonnull final String displayName) {
    return findOrRegister(name, displayName, null);
  }

  @Nonnull
  public static HighlightDisplayKey findOrRegister(@Nonnull final String name, @Nonnull final String displayName, @Nullable final String id) {
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
      final Computable<String> computable = ourKeyToDisplayNameMap.get(key);
      return computable == null ? null : computable.compute();
    }
  }

  public static String getAlternativeID(@Nonnull HighlightDisplayKey key) {
    return ourKeyToAlternativeIDMap.get(key);
  }


  private HighlightDisplayKey(@Nonnull final String name) {
    this(name, name);
  }

  public HighlightDisplayKey(@Nonnull final String name, @Nonnull final String ID) {
    myName = name;
    myID = ID;
    ourNameToKeyMap.put(myName, this);
    if (!Comparing.equal(ID, name)) {
      ourIdToKeyMap.put(ID, this);
    }
  }

  public String toString() {
    return myName;
  }

  @Nonnull
  public String getID() {
    return myID;
  }
}
