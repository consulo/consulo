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

package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplatesScheme;
import com.intellij.ide.fileTemplates.InternalTemplateBean;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import consulo.components.impl.stores.StorageUtil;
import consulo.logging.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
@State(name = "FileTemplateManagerImpl", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class FileTemplateManagerImpl extends FileTemplateManager implements PersistentStateComponent<FileTemplateManagerImpl.State> {
  private static final Logger LOG = Logger.getInstance(FileTemplateManagerImpl.class);

  private final State myState = new State();
  private final FileTypeManagerEx myTypeManager;
  private final FileTemplateSettings myProjectSettings;
  private final ExportableFileTemplateSettings myDefaultSettings;
  private final Project myProject;

  private final FileTemplatesScheme myProjectScheme;
  private FileTemplatesScheme myScheme = FileTemplatesScheme.DEFAULT;
  private boolean myInitialized;

  public static FileTemplateManagerImpl getInstanceImpl(@Nonnull Project project) {
    return (FileTemplateManagerImpl)getInstance(project);
  }

  @Inject
  public FileTemplateManagerImpl(@Nonnull FileTypeManager typeManager,
                                 FileTemplateSettings projectSettings,
                                 ExportableFileTemplateSettings defaultSettings,
                                 ProjectManager pm,
                                 final Project project) {
    myTypeManager = (FileTypeManagerEx)typeManager;
    myProjectSettings = projectSettings;
    myDefaultSettings = defaultSettings;
    myProject = project;

    myProjectScheme = project.isDefault() ? null : new FileTemplatesScheme("Project") {
      @Nonnull
      @Override
      public String getTemplatesDir() {
        return FileUtilRt.toSystemDependentName(StorageUtil.getStoreDir(project) + "/" + TEMPLATES_DIR);
      }

      @Nonnull
      @Override
      public Project getProject() {
        return project;
      }
    };
  }

  private FileTemplateSettings getSettings() {
    return myScheme == FileTemplatesScheme.DEFAULT ? myDefaultSettings : myProjectSettings;
  }

  @Nonnull
  @Override
  public FileTemplatesScheme getCurrentScheme() {
    return myScheme;
  }

  @Override
  public void setCurrentScheme(@Nonnull FileTemplatesScheme scheme) {
    for (FTManager child : getAllManagers()) {
      child.saveTemplates();
    }
    setScheme(scheme);
  }

  private void setScheme(@Nonnull FileTemplatesScheme scheme) {
    myScheme = scheme;
    myInitialized = true;
  }

  @Override
  protected FileTemplateManager checkInitialized() {
    if (!myInitialized) {
      // loadState() not called; init default scheme
      setScheme(myScheme);
    }
    return this;
  }

  @Nullable
  @Override
  public FileTemplatesScheme getProjectScheme() {
    return myProjectScheme;
  }

  @Override
  public FileTemplate[] getTemplates(String category) {
    if (DEFAULT_TEMPLATES_CATEGORY.equals(category)) return ArrayUtil.mergeArrays(getInternalTemplates(), getAllTemplates());
    if (INCLUDES_TEMPLATES_CATEGORY.equals(category)) return getAllPatterns();
    if (CODE_TEMPLATES_CATEGORY.equals(category)) return getAllCodeTemplates();
    if (J2EE_TEMPLATES_CATEGORY.equals(category)) return getAllJ2eeTemplates();
    throw new IllegalArgumentException("Unknown category: " + category);
  }

  @Override
  @Nonnull
  public FileTemplate[] getAllTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
    return templates.toArray(new FileTemplate[templates.size()]);
  }

  @Override
  public FileTemplate getTemplate(@Nonnull String templateName) {
    return getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
  }

  @Override
  @Nonnull
  public FileTemplate addTemplate(@Nonnull String name, @Nonnull String extension) {
    return getSettings().getDefaultTemplatesManager().addTemplate(name, extension);
  }

  @Override
  public void removeTemplate(@Nonnull FileTemplate template) {
    final String qName = ((FileTemplateBase)template).getQualifiedName();
    for (FTManager manager : getAllManagers()) {
      manager.removeTemplate(qName);
    }
  }

  @Override
  @Nonnull
  public Properties getDefaultProperties() {
    Properties props = new Properties();
    for (Map.Entry<String, Object> entry : getDefaultVariables().entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
    return props;
  }

  @Override
  public void fillDefaultVariables(@Nonnull Map<String, Object> map) {
    Calendar calendar = Calendar.getInstance();
    Date date = myTestDate == null ? calendar.getTime() : myTestDate;
    SimpleDateFormat sdfMonthNameShort = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthNameFull = new SimpleDateFormat("MMMM");
    SimpleDateFormat sdfDayNameShort = new SimpleDateFormat("EEE");
    SimpleDateFormat sdfDayNameFull = new SimpleDateFormat("EEEE");
    SimpleDateFormat sdfYearFull = new SimpleDateFormat("yyyy");

    map.put("DATE", DateFormatUtil.formatDate(date));
    map.put("TIME", DateFormatUtil.formatTime(date));
    map.put("YEAR", sdfYearFull.format(date));
    map.put("MONTH", getCalendarValue(calendar, Calendar.MONTH));
    map.put("MONTH_NAME_SHORT", sdfMonthNameShort.format(date));
    map.put("MONTH_NAME_FULL", sdfMonthNameFull.format(date));
    map.put("DAY", getCalendarValue(calendar, Calendar.DAY_OF_MONTH));
    map.put("DAY_NAME_SHORT", sdfDayNameShort.format(date));
    map.put("DAY_NAME_FULL", sdfDayNameFull.format(date));
    map.put("HOUR", getCalendarValue(calendar, Calendar.HOUR_OF_DAY));
    map.put("MINUTE", getCalendarValue(calendar, Calendar.MINUTE));
    map.put("SECOND", getCalendarValue(calendar, Calendar.SECOND));

    map.put("USER", SystemProperties.getUserName());
    map.put("PRODUCT_NAME", ApplicationNamesInfo.getInstance().getFullProductName());

    map.put("DS", "$"); // Dollar sign, strongly needed for PHP, JS, etc. See WI-8979

    map.put(PROJECT_NAME_VARIABLE, myProject.getName());
  }

  @Nonnull
  private static String getCalendarValue(final Calendar calendar, final int field) {
    int val = calendar.get(field);
    if (field == Calendar.MONTH) val++;
    final String result = Integer.toString(val);
    if (result.length() == 1) {
      return "0" + result;
    }
    return result;
  }

  @Override
  @Nonnull
  public Collection<String> getRecentNames() {
    validateRecentNames(); // todo: no need to do it lazily
    return myState.getRecentNames(RECENT_TEMPLATES_SIZE);
  }

  @Override
  public void addRecentName(@Nonnull @NonNls String name) {
    myState.addName(name);
  }

  private void validateRecentNames() {
    final Collection<FileTemplateBase> allTemplates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
    final List<String> allNames = new ArrayList<>(allTemplates.size());
    for (FileTemplate fileTemplate : allTemplates) {
      allNames.add(fileTemplate.getName());
    }
    myState.validateNames(allNames);
  }

  @Override
  @Nonnull
  public FileTemplate[] getInternalTemplates() {
    InternalTemplateBean[] internalTemplateBeans = Extensions.getExtensions(InternalTemplateBean.EP_NAME);
    FileTemplate[] result = new FileTemplate[internalTemplateBeans.length];
    for (int i = 0; i < internalTemplateBeans.length; i++) {
      result[i] = getInternalTemplate(internalTemplateBeans[i].name);
    }
    return result;
  }

  @Override
  public FileTemplate getInternalTemplate(@Nonnull @NonNls String templateName) {
    FileTemplateBase template = (FileTemplateBase)findInternalTemplate(templateName);

    if (template == null) {
      template = (FileTemplateBase)getJ2eeTemplate(templateName); // Hack to be able to register class templates from the plugin.
      if (template != null) {
        template.setReformatCode(true);
      }
      else {
        final String text = normalizeText(getDefaultClassTemplateText(templateName));
        template = getSettings().getInternalTemplatesManager().addTemplate(templateName, "java");
        template.setText(text);
      }
    }
    return template;
  }

  @Override
  public FileTemplate findInternalTemplate(@Nonnull @NonNls String templateName) {
    FileTemplateBase template = getSettings().getInternalTemplatesManager().findTemplateByName(templateName);

    if (template == null) {
      // todo: review the hack and try to get rid of this weird logic completely
      template = getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
    }
    return template;
  }

  @Nonnull
  public static String normalizeText(@Nonnull String text) {
    text = StringUtil.convertLineSeparators(text);
    text = StringUtil.replace(text, "$NAME$", "${NAME}");
    text = StringUtil.replace(text, "$PACKAGE_NAME$", "${PACKAGE_NAME}");
    text = StringUtil.replace(text, "$DATE$", "${DATE}");
    text = StringUtil.replace(text, "$TIME$", "${TIME}");
    text = StringUtil.replace(text, "$USER$", "${USER}");
    return text;
  }

  @Override
  @Nonnull
  public String internalTemplateToSubject(@Nonnull @NonNls String templateName) {
    //noinspection HardCodedStringLiteral
    for (InternalTemplateBean bean : Extensions.getExtensions(InternalTemplateBean.EP_NAME)) {
      if (bean.name.equals(templateName) && bean.subject != null) {
        return bean.subject;
      }
    }
    return templateName.toLowerCase();
  }

  @Override
  @Nonnull
  public String localizeInternalTemplateName(@Nonnull final FileTemplate template) {
    return template.getName();
  }

  @NonNls
  @Nonnull
  private String getDefaultClassTemplateText(@Nonnull @NonNls String templateName) {
    return IdeBundle.message("template.default.class.comment", ApplicationNamesInfo.getInstance().getFullProductName()) +
           "package $PACKAGE_NAME$;\n" +
           "public " +
           internalTemplateToSubject(templateName) +
           " $NAME$ { }";
  }

  @Override
  public FileTemplate getCodeTemplate(@Nonnull @NonNls String templateName) {
    return getTemplateFromManager(templateName, getSettings().getCodeTemplatesManager());
  }

  @Override
  public FileTemplate getJ2eeTemplate(@Nonnull @NonNls String templateName) {
    return getTemplateFromManager(templateName, getSettings().getJ2eeTemplatesManager());
  }

  @Nullable
  private static FileTemplate getTemplateFromManager(@Nonnull final String templateName, @Nonnull final FTManager ftManager) {
    FileTemplateBase template = ftManager.getTemplate(templateName);
    if (template != null) {
      return template;
    }
    template = ftManager.findTemplateByName(templateName);
    if (template != null) {
      return template;
    }
    if (templateName.endsWith("ForTest") && ApplicationManager.getApplication().isUnitTestMode()) {
      return null;
    }

    String message = "Template not found: " + templateName/*ftManager.templateNotFoundMessage(templateName)*/;
    LOG.error(message);
    return null;
  }

  @Override
  @Nonnull
  public FileTemplate getDefaultTemplate(@Nonnull final String name) {
    final String templateQName = myTypeManager.getExtension(name).isEmpty() ? FileTemplateBase.getQualifiedName(name, "java") : name;

    for (FTManager manager : getAllManagers()) {
      final FileTemplateBase template = manager.getTemplate(templateQName);
      if (template instanceof BundledFileTemplate) {
        final BundledFileTemplate copy = ((BundledFileTemplate)template).clone();
        copy.revertToDefaults();
        return copy;
      }
    }

    String message = "Default template not found: " + name;
    LOG.error(message);
    return null;
  }

  @Override
  @Nonnull
  public FileTemplate[] getAllPatterns() {
    final Collection<FileTemplateBase> allTemplates = getSettings().getPatternsManager().getAllTemplates(false);
    return allTemplates.toArray(new FileTemplate[allTemplates.size()]);
  }

  @Override
  public FileTemplate getPattern(@Nonnull String name) {
    return getSettings().getPatternsManager().findTemplateByName(name);
  }

  @Override
  @Nonnull
  public FileTemplate[] getAllCodeTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getCodeTemplatesManager().getAllTemplates(false);
    return templates.toArray(new FileTemplate[templates.size()]);
  }

  @Override
  @Nonnull
  public FileTemplate[] getAllJ2eeTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getJ2eeTemplatesManager().getAllTemplates(false);
    return templates.toArray(new FileTemplate[templates.size()]);
  }

  @Override
  public void setTemplates(@Nonnull String templatesCategory, @Nonnull Collection<FileTemplate> templates) {
    for (FTManager manager : getAllManagers()) {
      if (templatesCategory.equals(manager.getName())) {
        manager.updateTemplates(templates);
        break;
      }
    }
  }

  @Override
  public void saveAllTemplates() {
    for (FTManager manager : getAllManagers()) {
      manager.saveTemplates();
    }
  }

  public URL getDefaultTemplateDescription() {
    return myDefaultSettings.getDefaultTemplateDescription();
  }

  public URL getDefaultIncludeDescription() {
    return myDefaultSettings.getDefaultIncludeDescription();
  }

  private Date myTestDate;

  @TestOnly
  public void setTestDate(Date testDate) {
    myTestDate = testDate;
  }

  @Nullable
  @Override
  public State getState() {
    myState.SCHEME = myScheme.getName();
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
    FileTemplatesScheme scheme = myProjectScheme != null && myProjectScheme.getName().equals(state.SCHEME) ? myProjectScheme : FileTemplatesScheme.DEFAULT;
    setScheme(scheme);
  }

  private FTManager[] getAllManagers() {
    return getSettings().getAllManagers();
  }

  public static class State {
    public List<String> RECENT_TEMPLATES = new ArrayList<>();
    public String SCHEME = FileTemplatesScheme.DEFAULT.getName();

    public void addName(@Nonnull @NonNls String name) {
      RECENT_TEMPLATES.remove(name);
      RECENT_TEMPLATES.add(name);
    }

    @Nonnull
    public Collection<String> getRecentNames(int max) {
      int size = RECENT_TEMPLATES.size();
      int resultSize = Math.min(max, size);
      return RECENT_TEMPLATES.subList(size - resultSize, size);
    }

    public void validateNames(List<String> validNames) {
      RECENT_TEMPLATES.retainAll(validNames);
    }
  }
}
