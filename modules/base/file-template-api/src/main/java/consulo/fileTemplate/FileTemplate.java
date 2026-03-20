/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.fileTemplate;

import consulo.annotation.DeprecationInfo;
import consulo.project.Project;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.fileType.FileType;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author MYakovlev
 * @since 2002-07-24
 */
public interface FileTemplate extends Cloneable {
  FileTemplate[] EMPTY_ARRAY = new FileTemplate[0];

  String ourEncoding = CharsetToolkit.UTF8;

  String ATTRIBUTE_EXCEPTION = "EXCEPTION";
  String ATTRIBUTE_EXCEPTION_TYPE = "EXCEPTION_TYPE";
  String ATTRIBUTE_DESCRIPTION = "DESCRIPTION";
  String ATTRIBUTE_DISPLAY_NAME = "DISPLAY_NAME";

  String ATTRIBUTE_RETURN_TYPE = "RETURN_TYPE";
  String ATTRIBUTE_DEFAULT_RETURN_VALUE = "DEFAULT_RETURN_VALUE";
  String ATTRIBUTE_CALL_SUPER = "CALL_SUPER";

  String ATTRIBUTE_CLASS_NAME = "CLASS_NAME";
  String ATTRIBUTE_SIMPLE_CLASS_NAME = "SIMPLE_CLASS_NAME";
  String ATTRIBUTE_METHOD_NAME = "METHOD_NAME";
  String ATTRIBUTE_PACKAGE_NAME = "PACKAGE_NAME";
  String ATTRIBUTE_NAME = "NAME";
  String ATTRIBUTE_FILE_NAME = "FILE_NAME";

  /**
   * Name without extension
   */
  String getName();

  void setName(String name);

  boolean isTemplateOfType(FileType fType);

  boolean isDefault();

  
  String getDescription();

  
  String getText();

  void setText(String text);

  
  String getText(Map attributes) throws IOException;

  
  String getText(Properties attributes) throws IOException;

  
  String getExtension();

  void setExtension(String extension);

  boolean isReformatCode();

  void setReformatCode(boolean reformat);

  boolean isLiveTemplateEnabled();

  void setLiveTemplateEnabled(boolean value);

  FileTemplate clone();

  
  @Deprecated
  @DeprecationInfo("getUnsetAttributes")
  String[] getUnsetAttributes(Properties properties, boolean includeDummies, Project project) throws FileTemplateParseException;

  
  String[] getUnsetAttributes(Map<String, Object> properties, boolean includeDummies, Project project) throws FileTemplateParseException;

  
  @Deprecated
  @DeprecationInfo("getUnsetAttributes")
  default String[] getUnsetAttributes(Properties properties, Project project) throws FileTemplateParseException {
    return getUnsetAttributes(properties, false, project);
  }

  
  default String[] getUnsetAttributes(Map<String, Object> properties, Project project) throws FileTemplateParseException {
    return getUnsetAttributes(properties, false, project);
  }
}