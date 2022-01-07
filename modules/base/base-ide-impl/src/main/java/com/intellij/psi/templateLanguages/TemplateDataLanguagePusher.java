// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.FileIntPropertyPusher;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.VfsDependentEnum;
import com.intellij.util.ObjectUtils;
import com.intellij.util.io.EnumeratorStringDescriptor;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

/**
 * @author Konstantin.Ulitin
 */
public class TemplateDataLanguagePusher implements FileIntPropertyPusher<Language> {

  public static final Key<Language> KEY = Key.create("TEMPLATE_DATA_LANGUAGE");

  private static final VfsDependentEnum<String> ourLanguagesEnumerator = new VfsDependentEnum<>("languages", EnumeratorStringDescriptor.INSTANCE, 1);

  @Nonnull
  @Override
  public Key<Language> getFileDataKey() {
    return KEY;
  }

  @Override
  public boolean pushDirectoriesOnly() {
    return false;
  }

  @Nonnull
  @Override
  public Language getDefaultValue() {
    return Language.ANY;
  }

  @Nullable
  @Override
  public Language getImmediateValue(@Nonnull Project project, @Nullable VirtualFile file) {
    return TemplateDataLanguageMappings.getInstance(project).getImmediateMapping(file);
  }

  @Nullable
  @Override
  public Language getImmediateValue(@Nonnull Module module) {
    return null;
  }

  @Override
  public boolean acceptsFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    FileType type = file.getFileType();
    return type instanceof LanguageFileType && ((LanguageFileType)type).getLanguage() instanceof TemplateLanguage;
  }

  @Override
  public boolean acceptsDirectory(@Nonnull VirtualFile file, @Nonnull Project project) {
    return true;
  }

  private static final FileAttribute PERSISTENCE = new FileAttribute("template_language", 2, true);

  @Override
  public
  @Nonnull
  FileAttribute getAttribute() {
    return PERSISTENCE;
  }

  @Override
  public int toInt(@Nonnull Language property) throws IOException {
    return ourLanguagesEnumerator.getId(property.getID());
  }

  @Nonnull
  @Override
  public Language fromInt(int val) throws IOException {
    String id = ourLanguagesEnumerator.getById(val);
    Language lang = Language.findLanguageByID(id);
    return ObjectUtils.notNull(lang, Language.ANY);
  }

  @Override
  public void propertyChanged(@Nonnull Project project, @Nonnull VirtualFile fileOrDir, @Nonnull Language actualProperty) {
    PushedFilePropertiesUpdater.getInstance(project).filePropertiesChanged(fileOrDir, file -> acceptsFile(file, project));
  }
}
