// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.language.template.TemplateLanguage;
import consulo.module.Module;
import consulo.module.content.FileStringPropertyPusher;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import org.jspecify.annotations.Nullable;
import java.io.IOException;

/**
 * @author Konstantin.Ulitin
 */
@ExtensionImpl
public class TemplateDataLanguagePusher implements FileStringPropertyPusher<Language> {

  public static final Key<Language> KEY = Key.create("TEMPLATE_DATA_LANGUAGE");

  
  @Override
  public Key<Language> getFileDataKey() {
    return KEY;
  }

  @Override
  public boolean pushDirectoriesOnly() {
    return false;
  }

  
  @Override
  public Language getDefaultValue() {
    return Language.ANY;
  }

  @Override
  public @Nullable Language getImmediateValue(Project project, @Nullable VirtualFile file) {
    return TemplateDataLanguageMappings.getInstance(project).getImmediateMapping(file);
  }

  @Override
  public @Nullable Language getImmediateValue(Module module) {
    return null;
  }

  @Override
  public boolean acceptsFile(VirtualFile file, Project project) {
    FileType type = file.getFileType();
    return type instanceof LanguageFileType && ((LanguageFileType)type).getLanguage() instanceof TemplateLanguage;
  }

  @Override
  public boolean acceptsDirectory(VirtualFile file, Project project) {
    return true;
  }

  private static final FileAttribute PERSISTENCE = new FileAttribute("template_language", 2, true);

  @Override
  
  public FileAttribute getAttribute() {
    return PERSISTENCE;
  }

  @Override
  public String toString(Language property) throws IOException {
    return property.getID();
  }

  
  @Override
  public Language fromString(String val) throws IOException {
    Language language = Language.findLanguageByID(val);
    return language == null ? Language.ANY : language;
  }

  @Override
  public void propertyChanged(Project project, VirtualFile fileOrDir, Language actualProperty) {
    PushedFilePropertiesUpdater.getInstance(project).filePropertiesChanged(fileOrDir, file -> acceptsFile(file, project));
  }
}
