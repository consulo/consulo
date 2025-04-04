/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.inject.advanced;

import consulo.language.Language;
import consulo.language.util.LanguageUtil;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public final class InjectedLanguage {
  private static Map<String, Language> ourLanguageCache;
  private static int ourLanguageCount;

  private final String myID;
  private final String myPrefix;
  private final String mySuffix;
  private final boolean myDynamic;

  private InjectedLanguage(@Nonnull String id, @Nonnull String prefix, @Nonnull String suffix, boolean dynamic) {
    myID = id;
    myPrefix = prefix;
    mySuffix = suffix;
    myDynamic = dynamic;
  }

  @Nonnull
  public String getID() {
    return myID;
  }

  @Nullable
  public Language getLanguage() {
    return findLanguageById(myID);
  }

  @Nonnull
  public String getPrefix() {
    return myPrefix;
  }

  @Nonnull
  public String getSuffix() {
    return mySuffix;
  }

  /**
   * Returns whether prefix/suffix were computed dynamically
   */
  public boolean isDynamic() {
    return myDynamic;
  }

  @Nullable
  public static Language findLanguageById(@Nullable String langID) {
    if (langID == null || langID.length() == 0) {
      return null;
    }
    synchronized (InjectedLanguage.class) {
      if (ourLanguageCache == null || ourLanguageCount != Language.getRegisteredLanguages().size()) {
        initLanguageCache();
      }
      return ourLanguageCache.get(langID);
    }
  }

  @Nonnull
  public static String[] getAvailableLanguageIDs() {
    synchronized (InjectedLanguage.class) {
      if (ourLanguageCache == null || ourLanguageCount != Language.getRegisteredLanguages().size()) {
        initLanguageCache();
      }
      final Set<String> keys = ourLanguageCache.keySet();
      return ArrayUtil.toStringArray(keys);
    }
  }

  @Nonnull
  public static Language[] getAvailableLanguages() {
    synchronized (InjectedLanguage.class) {
      if (ourLanguageCache == null || ourLanguageCount != Language.getRegisteredLanguages().size()) {
        initLanguageCache();
      }
      final Collection<Language> keys = ourLanguageCache.values();
      return keys.toArray(new Language[keys.size()]);
    }
  }

  private static void initLanguageCache() {
    ourLanguageCache = new HashMap<String, Language>();

    Collection<Language> registeredLanguages;
    do {
      registeredLanguages = new ArrayList<Language>(Language.getRegisteredLanguages());
      for (Language language : registeredLanguages) {
        if (LanguageUtil.isInjectableLanguage(language)) {
          ourLanguageCache.put(language.getID(), language);
        }
      }
    } while (Language.getRegisteredLanguages().size() != registeredLanguages.size());

    ourLanguageCount = registeredLanguages.size();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final InjectedLanguage that = (InjectedLanguage)o;

    return !(myID != null ? !myID.equals(that.myID) : that.myID != null);
  }

  public int hashCode() {
    return (myID != null ? myID.hashCode() : 0);
  }

  @Nullable
  public static InjectedLanguage create(String id) {
    return create(id, "", "", false);
  }

  @Nullable
  public static InjectedLanguage create(@Nullable String id, String prefix, String suffix, boolean isDynamic) {
    return id == null ? null : new InjectedLanguage(id, prefix == null ? "" : prefix, suffix == null ? "" : suffix, isDynamic);
  }
}
