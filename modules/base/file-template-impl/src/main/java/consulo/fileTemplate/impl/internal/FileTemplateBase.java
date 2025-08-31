/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.fileTemplate.impl.internal;

import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateParseException;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Eugene Zhuravlev
 * @since 2011-04-06
 */
public abstract class FileTemplateBase implements FileTemplate {
  public static final boolean DEFAULT_REFORMAT_CODE_VALUE = true;
  public static final boolean DEFAULT_ENABLED_VALUE = true;
  @Nullable
  private String myText;
  private boolean myShouldReformatCode = DEFAULT_REFORMAT_CODE_VALUE;
  private boolean myLiveTemplateEnabled;

  @Override
  public final boolean isReformatCode() {
    return myShouldReformatCode;
  }

  @Override
  public final void setReformatCode(boolean reformat) {
    myShouldReformatCode = reformat;
  }

  public final String getQualifiedName() {
    return getQualifiedName(getName(), getExtension());
  }

  public static String getQualifiedName(String name, String extension) {
    return FTManager.encodeFileName(name, extension);
  }

  @Override
  @Nonnull
  public final String getText() {
    String text = myText;
    return text != null? text : getDefaultText();
  }

  @Override
  public final void setText(@Nullable String text) {
    if (text == null) {
      myText = null;
    }
    else {
      String converted = StringUtil.convertLineSeparators(text);
      myText = converted.equals(getDefaultText())? null : converted;
    }
  }

  @Nonnull
  protected String getDefaultText() {
    return "";
  }

  @Override
  @Nonnull
  public final String getText(Map attributes) throws IOException{
    return FileTemplateImplUtil.mergeTemplate(attributes, getText(), false);
  }

  @Override
  @Nonnull
  public final String getText(Properties attributes) throws IOException{
    return FileTemplateImplUtil.mergeTemplate(attributes, getText(), false);
  }

  @Override
  @Nonnull
  public final String[] getUnsetAttributes(@Nonnull Properties properties, boolean includeDummies, Project project) throws FileTemplateParseException {
    return FileTemplateImplUtil.calculateAttributes(getText(), properties, includeDummies, project);
  }

  @Override
  @Nonnull
  public final String[] getUnsetAttributes(@Nonnull Map<String, Object> properties, boolean includeDummies, Project project) throws FileTemplateParseException {
    return FileTemplateImplUtil.calculateAttributes(getText(), properties, includeDummies, project);
  }

  @Override
  public FileTemplateBase clone() {
    try {
      return (FileTemplateBase)super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isTemplateOfType(@Nonnull FileType fType) {
    return fType.equals(FileTypeRegistry.getInstance().getFileTypeByExtension(getExtension()));
  }

  @Override
  public boolean isLiveTemplateEnabled() {
    return myLiveTemplateEnabled;
  }

  @Override
  public void setLiveTemplateEnabled(boolean value) {
    myLiveTemplateEnabled = value;
  }

  public boolean isLiveTemplateEnabledByDefault() { return false; }
}
