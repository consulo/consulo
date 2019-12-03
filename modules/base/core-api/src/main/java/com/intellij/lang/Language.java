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
package com.intellij.lang;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.MultiMap;
import consulo.annotation.UsedInPlugin;
import consulo.lang.LanguageVersion;
import consulo.lang.LanguageVersionDefines;
import consulo.logging.Logger;
import consulo.util.pointers.Named;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The base class for all programming language support implementations. Specific language implementations should inherit from this class
 * and its register instance wrapped with {@link LanguageFileType} instance through
 * <code>FileTypeManager.getInstance().registerFileType</code>
 * There should be exactly one instance of each Language.
 * It is usually created when creating {@link LanguageFileType} and can be retrieved later
 * with {@link #findInstance(Class)}.
 */
public abstract class Language extends UserDataHolderBase implements Named {
  private static final Logger LOG = Logger.getInstance(Language.class);

  // this fields must be before ANY field due registration
  private static final Map<Class<? extends Language>, Language> ourRegisteredLanguages = new ConcurrentHashMap<>();
  private static final Map<String, Language> ourRegisteredIDs = new ConcurrentHashMap<>();

  public static final Language ANY = new Language("") {
    @Override
    public String toString() {
      return "Language: ANY";
    }
  };

  private static final NotNullLazyValue<MultiMap<String, LanguageVersion>> ourRegisteredMimeTypesValue = NotNullLazyValue.createValue(() -> {
    MultiMap<String, LanguageVersion> mimeTypesMap = new MultiMap<>();

    Collection<Language> registeredLanguages = getRegisteredLanguages();

    for (Language language : registeredLanguages) {
      LanguageVersion[] versions = language.getVersions();
      for (LanguageVersion version : versions) {
        for (String mimeType : version.getMimeTypes()) {
          mimeTypesMap.putValue(mimeType, version);
        }
      }

      String[] mimeTypes = language.getMimeTypes();
      for (String mimeType : mimeTypes) {
        for (LanguageVersion version : versions) {
          mimeTypesMap.putValue(mimeType, version);
        }
      }
    }

    return mimeTypesMap;
  });

  private final Language myBaseLanguage;
  private final String myID;
  private final String[] myMimeTypes;

  private NotNullLazyValue<LanguageVersion[]> myVersions = NotNullLazyValue.createValue(() -> {
    LanguageVersion[] versions = findVersions();
    if (versions.length == 0) {
      throw new IllegalArgumentException("Language version is empty for language: " + Language.this);
    }
    return versions;
  });

  protected Language(@Nonnull @NonNls String id) {
    this(id, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  protected Language(@Nonnull @NonNls final String ID, @Nonnull @NonNls final String... mimeTypes) {
    this(null, ID, mimeTypes);
  }

  protected Language(@Nullable Language baseLanguage, @Nonnull @NonNls final String ID, @Nonnull @NonNls final String... mimeTypes) {
    myBaseLanguage = baseLanguage;
    myID = ID;
    myMimeTypes = mimeTypes;
    Class<? extends Language> langClass = getClass();
    Language prev = ourRegisteredLanguages.put(langClass, this);
    if (prev != null) {
      LOG.error("Language of '" + langClass + "' is already registered: " + prev);
      return;
    }
    prev = ourRegisteredIDs.put(ID, this);
    if (prev != null) {
      LOG.error("Language with ID '" + ID + "' is already registered: " + prev.getClass());
    }
  }

  /**
   * Fake language identifier without registering
   */
  protected Language(String id, @SuppressWarnings("UnusedParameters") boolean register) {
    myID = id;
    myBaseLanguage = null;
    myMimeTypes = ArrayUtil.EMPTY_STRING_ARRAY;
  }

  /**
   * @return collection of all languages registered so far.
   */
  @Nonnull
  public static Collection<Language> getRegisteredLanguages() {
    return Collections.unmodifiableCollection(ourRegisteredLanguages.values());
  }

  /**
   * @param klass <code>java.lang.Class</code> of the particular language. Serves key purpose.
   * @return instance of the <code>klass</code> language registered if any.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends Language> T findInstance(Class<T> klass) {
    return (T)ourRegisteredLanguages.get(klass);
  }

  /**
   * @param mimeType of the particular language.
   * @return collection of all languages for the given <code>mimeType</code>.
   */
  @Nonnull
  @UsedInPlugin
  public static Collection<Language> findInstancesByMimeType(@Nullable String mimeType) {
    if(mimeType == null) {
      return Collections.emptyList();
    }

    Collection<LanguageVersion> versions = ourRegisteredMimeTypesValue.getValue().get(mimeType);
    Set<Language> languages = new HashSet<>();
    for (LanguageVersion version : versions) {
      languages.add(version.getLanguage());
    }
    return languages;
  }

  @Nonnull
  @UsedInPlugin
  public static Collection<LanguageVersion> findVersionsByMimeType(@Nullable String mimeType) {
    MultiMap<String, LanguageVersion> map = ourRegisteredMimeTypesValue.getValue();
    return Collections.unmodifiableCollection(map.get(mimeType));
  }

  @Nonnull
  protected LanguageVersion[] findVersions() {
    List<LanguageVersion> languageVersion = LanguageVersionDefines.INSTANCE.allForLanguage(this);
    if (languageVersion.isEmpty()) {
      return new LanguageVersion[]{new LanguageVersion("DEFAULT", "Default", this)};
    }
    Collections.reverse(languageVersion);
    return languageVersion.toArray(new LanguageVersion[languageVersion.size()]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Language)) return false;
    Language language = (Language)o;
    return Objects.equals(myID, language.myID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myID);
  }

  @Override
  public String toString() {
    //noinspection HardCodedStringLiteral
    return "Language: " + myID;
  }

  /**
   * Returns the list of MIME types corresponding to the language.
   *
   * @return The list of MIME types.
   */
  @Nonnull
  public String[] getMimeTypes() {
    return myMimeTypes;
  }

  /**
   * Returns a id of the language.
   *
   * @return the name of the language.
   */
  @Nonnull
  public String getID() {
    return myID;
  }

  @Nonnull
  @Override
  public String getName() {
    return getID();
  }

  @Nullable
  public LanguageFileType getAssociatedFileType() {
    final FileType[] types = FileTypeRegistry.getInstance().getRegisteredFileTypes();
    for (final FileType fileType : types) {
      if (fileType instanceof LanguageFileType && ((LanguageFileType)fileType).getLanguage() == this) {
        return (LanguageFileType)fileType;
      }
    }
    for (final FileType fileType : types) {
      if (fileType instanceof LanguageFileType && isKindOf(((LanguageFileType)fileType).getLanguage())) {
        return (LanguageFileType)fileType;
      }
    }
    return null;
  }

  @Nullable
  //@ApiStatus.Internal
  public LanguageFileType findMyFileType(FileType[] types) {
    for (final FileType fileType : types) {
      if (fileType instanceof LanguageFileType) {
        final LanguageFileType languageFileType = (LanguageFileType)fileType;
        if (languageFileType.getLanguage() == this && !languageFileType.isSecondary()) {
          return languageFileType;
        }
      }
    }
    for (final FileType fileType : types) {
      if (fileType instanceof LanguageFileType) {
        final LanguageFileType languageFileType = (LanguageFileType)fileType;
        if (isKindOf(languageFileType.getLanguage()) && !languageFileType.isSecondary()) {
          return languageFileType;
        }
      }
    }
    return null;
  }

  @Nullable
  public Language getBaseLanguage() {
    return myBaseLanguage;
  }

  @Nonnull
  public final LanguageVersion[] getVersions() {
    return myVersions.getValue();
  }

  @Nullable
  @SuppressWarnings("unchecked")
  @UsedInPlugin
  public <T extends LanguageVersion> T findVersionByClass(@Nonnull Class<T> clazz) {
    for (LanguageVersion languageVersion : getVersions()) {
      if (languageVersion.getClass() == clazz) {
        return (T)languageVersion;
      }
    }
    return null;
  }

  public String getDisplayName() {
    return getID();
  }

  public final boolean is(Language another) {
    return this == another;
  }

  public boolean isCaseSensitive() {
    return myBaseLanguage != null && myBaseLanguage.isCaseSensitive();
  }

  public final boolean isKindOf(Language another) {
    Language l = this;
    while (l != null) {
      if (l.is(another)) return true;
      l = l.getBaseLanguage();
    }
    return false;
  }

  @UsedInPlugin
  public final boolean isKindOf(String anotherLanguageId) {
    Language l = this;
    while (l != null) {
      if (l.getID().equals(anotherLanguageId)) return true;
      l = l.getBaseLanguage();
    }
    return false;
  }

  @Nullable
  public static Language findLanguageByID(String id) {
    final Collection<Language> languages = getRegisteredLanguages();
    for (Language language : languages) {
      if (language.getID().equals(id)) {
        return language;
      }
    }
    return null;
  }
}
