// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.templateLanguages;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.FileIntPropertyPusher;
import consulo.language.Language;
import consulo.language.template.TemplateLanguage;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.LanguageFileType;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.roots.impl.PushedFilePropertiesUpdater;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.newvfs.FileAttribute;
import consulo.ide.impl.idea.openapi.vfs.newvfs.persistent.VfsDependentEnum;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

/**
 * @author Konstantin.Ulitin
 */
@ExtensionImpl
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
