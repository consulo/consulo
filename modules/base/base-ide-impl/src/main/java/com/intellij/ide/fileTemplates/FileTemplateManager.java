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

package com.intellij.ide.fileTemplates;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.annotation.DeprecationInfo;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author MYakovlev
 * Date: Jul 24, 2002
 */
public abstract class FileTemplateManager{
  public static final Key<Properties> DEFAULT_TEMPLATE_PROPERTIES = Key.create("DEFAULT_TEMPLATE_PROPERTIES");
  public static final int RECENT_TEMPLATES_SIZE = 25;

  @NonNls
  public static final String INTERNAL_HTML_TEMPLATE_NAME = "HTML4 File";
  @NonNls
  public static final String INTERNAL_HTML5_TEMPLATE_NAME = "HTML File";
  @NonNls
  public static final String INTERNAL_XHTML_TEMPLATE_NAME = "XHTML File";
  @NonNls
  public static final String FILE_HEADER_TEMPLATE_NAME = "File Header";

  public static final String DEFAULT_TEMPLATES_CATEGORY = "Default";
  public static final String INTERNAL_TEMPLATES_CATEGORY = "Internal";
  public static final String INCLUDES_TEMPLATES_CATEGORY = "Includes";
  public static final String CODE_TEMPLATES_CATEGORY = "Code";
  public static final String J2EE_TEMPLATES_CATEGORY = "J2EE";

  public static final String PROJECT_NAME_VARIABLE = "PROJECT_NAME";

  public static FileTemplateManager getInstance(@Nonnull Project project){
    return ServiceManager.getService(project, FileTemplateManager.class).checkInitialized();
  }

  protected FileTemplateManager checkInitialized() { return this; }

  /** Use {@link #getInstance(Project)} instead */
  @Deprecated
  public static FileTemplateManager getInstance(){
    return getDefaultInstance();
  }

  public static FileTemplateManager getDefaultInstance(){
    return getInstance(ProjectManager.getInstance().getDefaultProject());
  }

  @Nonnull
  public abstract FileTemplatesScheme getCurrentScheme();

  public abstract void setCurrentScheme(@Nonnull FileTemplatesScheme scheme);

  /**
   * @return Project scheme, or null if manager is created for default project.
   */
  public abstract FileTemplatesScheme getProjectScheme();

  public abstract FileTemplate[] getTemplates(String category);

  /**
   *  Returns all templates from "Default" category.
   */
  @Nonnull
  public abstract FileTemplate[] getAllTemplates();

  public abstract FileTemplate getTemplate(@Nonnull @NonNls String templateName);

  /**
   * @return a new Properties object filled with predefined properties.
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #getDefaultVariables()")
  public abstract Properties getDefaultProperties();

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #getDefaultVariables()")
  public Properties getDefaultProperties(@Nonnull Project project) {
    Properties properties = getDefaultProperties();
    properties.setProperty(PROJECT_NAME_VARIABLE, project.getName());
    return properties;
  }

  @Nonnull
  public Map<String, Object> getDefaultVariables() {
    Map<String, Object> map = new HashMap<>();
    fillDefaultVariables(map);
    return map;
  }

  public abstract void fillDefaultVariables(@Nonnull Map<String, Object> map);

  /**
   * Creates a new template with specified name, and adds it to the list of default templates.
   * @return created template
   */
  @Nonnull
  public abstract FileTemplate addTemplate(@Nonnull @NonNls String name, @Nonnull @NonNls String extension);

  public abstract void removeTemplate(@Nonnull FileTemplate template);

  @Nonnull
  public abstract Collection<String> getRecentNames();

  public abstract void addRecentName(@Nonnull @NonNls String name);

  public abstract FileTemplate getInternalTemplate(@Nonnull @NonNls String templateName);
  public abstract FileTemplate findInternalTemplate(@Nonnull @NonNls String templateName);

  @Nonnull
  public abstract FileTemplate[] getInternalTemplates();

  public abstract FileTemplate getJ2eeTemplate(@Nonnull @NonNls String templateName);
  public abstract FileTemplate getCodeTemplate(@Nonnull @NonNls String templateName);

  @Nonnull
  public abstract FileTemplate[] getAllPatterns();

  @Nonnull
  public abstract FileTemplate[] getAllCodeTemplates();

  @Nonnull
  public abstract FileTemplate[] getAllJ2eeTemplates();

  @Nonnull
  public abstract String internalTemplateToSubject(@Nonnull @NonNls String templateName);

  @Deprecated
  @Nonnull
  public abstract String localizeInternalTemplateName(@Nonnull FileTemplate template);

  public abstract FileTemplate getPattern(@Nonnull @NonNls String name);

  /**
   * Returns template with default (bundled) text.
   */
  @Nonnull
  public abstract FileTemplate getDefaultTemplate(@Nonnull @NonNls String name);

  public abstract void setTemplates(@Nonnull String templatesCategory, @Nonnull Collection<FileTemplate> templates);

  public abstract void saveAllTemplates();
}
