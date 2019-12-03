/*
 * Copyright 2013-2016 consulo.io
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
package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 17:59/30.05.13
 */
public class LanguageVersion {
  @Nonnull
  public static final Key<LanguageVersion> KEY = Key.create("LANGUAGE_VERSION");

  private final String myId;
  private final String myName;
  private final Language myLanguage;
  private final String[] myMimeTypes;

  public LanguageVersion(@Nonnull String id, @Nonnull String name, @Nonnull Language language, String... mimeTypes) {
    myId = id;
    myName = name;
    myLanguage = language;
    myMimeTypes = mimeTypes;
  }

  @Nonnull
  @NonNls
  public String getId() {
    return myId;
  }

  @Nonnull
  @NonNls
  public String getName() {
    return myName;
  }

  @Nonnull
  public Language getLanguage() {
    return myLanguage;
  }

  @Nonnull
  public String[] getMimeTypes() {
    return myMimeTypes;
  }

  @Nullable
  public FileType getAssociatedFileType() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LanguageVersion)) return false;
    LanguageVersion that = (LanguageVersion)o;
    return Objects.equals(myId, that.myId) && Objects.equals(myLanguage, that.myLanguage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myId, myLanguage);
  }

  @Override
  public String toString() {
    return "LanguageVersion: " + getId() + " for language: " + getLanguage();
  }
}
