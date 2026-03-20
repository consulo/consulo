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
package consulo.ide.impl.idea.ide.fileTemplates.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.DateFormatUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.FileTemplatesScheme;
import consulo.fileTemplate.impl.internal.*;
import consulo.ide.localize.IdeLocalize;
import consulo.language.file.FileTypeManager;
import consulo.language.internal.FileTypeManagerEx;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.impl.internal.ProjectStorageUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
@ServiceImpl
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

    public static FileTemplateManagerImpl getInstanceImpl(Project project) {
        return (FileTemplateManagerImpl) getInstance(project);
    }

    @Inject
    public FileTemplateManagerImpl(
        FileTypeManager typeManager,
        FileTemplateSettings projectSettings,
        ExportableFileTemplateSettings defaultSettings,
        ProjectManager pm,
        final Project project
    ) {
        myTypeManager = (FileTypeManagerEx) typeManager;
        myProjectSettings = projectSettings;
        myDefaultSettings = defaultSettings;
        myProject = project;

        myProjectScheme = project.isDefault() ? null : new FileTemplatesScheme("Project") {
            
            @Override
            public String getTemplatesDir() {
                return FileUtil.toSystemDependentName(ProjectStorageUtil.getStoreDir(project) + "/" + TEMPLATES_DIR);
            }

            
            @Override
            public Project getProject() {
                return project;
            }
        };
    }

    private FileTemplateSettingsBase getSettings() {
        return myScheme == FileTemplatesScheme.DEFAULT ? myDefaultSettings : myProjectSettings;
    }

    
    @Override
    public FileTemplatesScheme getCurrentScheme() {
        return myScheme;
    }

    @Override
    public void setCurrentScheme(FileTemplatesScheme scheme) {
        for (FTManager child : getAllManagers()) {
            child.saveTemplates();
        }
        setScheme(scheme);
    }

    private void setScheme(FileTemplatesScheme scheme) {
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

    @Override
    public @Nullable FileTemplatesScheme getProjectScheme() {
        return myProjectScheme;
    }

    @Override
    public FileTemplate[] getTemplates(String category) {
        return switch (category) {
            case DEFAULT_TEMPLATES_CATEGORY -> ArrayUtil.mergeArrays(getInternalTemplates(), getAllTemplates());
            case INCLUDES_TEMPLATES_CATEGORY -> getAllPatterns();
            case CODE_TEMPLATES_CATEGORY -> getAllCodeTemplates();
            case J2EE_TEMPLATES_CATEGORY -> getAllJ2eeTemplates();
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
    }

    @Override
    
    public FileTemplate[] getAllTemplates() {
        Collection<FileTemplateBase> templates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
        return templates.toArray(new FileTemplate[templates.size()]);
    }

    @Override
    public FileTemplate getTemplate(String templateName) {
        return getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
    }

    @Override
    
    public FileTemplate addTemplate(String name, String extension) {
        return getSettings().getDefaultTemplatesManager().addTemplate(name, extension);
    }

    @Override
    public void removeTemplate(FileTemplate template) {
        String qName = ((FileTemplateBase) template).getQualifiedName();
        for (FTManager manager : getAllManagers()) {
            manager.removeTemplate(qName);
        }
    }

    @Override
    
    public Properties getDefaultProperties() {
        Properties props = new Properties();
        for (Map.Entry<String, Object> entry : getDefaultVariables().entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        return props;
    }

    @Override
    public void fillDefaultVariables(Map<String, Object> map) {
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

        map.put("USER", Platform.current().user().name());
        map.put("PRODUCT_NAME", Application.get().getName().get());

        map.put("DS", "$"); // Dollar sign, strongly needed for PHP, JS, etc. See WI-8979

        map.put(PROJECT_NAME_VARIABLE, myProject.getName());
    }

    
    private static String getCalendarValue(Calendar calendar, int field) {
        int val = calendar.get(field);
        if (field == Calendar.MONTH) {
            val++;
        }
        String result = Integer.toString(val);
        if (result.length() == 1) {
            return "0" + result;
        }
        return result;
    }

    @Override
    
    public Collection<String> getRecentNames() {
        validateRecentNames(); // todo: no need to do it lazily
        return myState.getRecentNames(RECENT_TEMPLATES_SIZE);
    }

    @Override
    public void addRecentName(String name) {
        myState.addName(name);
    }

    private void validateRecentNames() {
        Collection<FileTemplateBase> allTemplates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
        List<String> allNames = new ArrayList<>(allTemplates.size());
        for (FileTemplate fileTemplate : allTemplates) {
            allNames.add(fileTemplate.getName());
        }
        myState.validateNames(allNames);
    }

    @Override
    
    public FileTemplate[] getInternalTemplates() {
        List<FileTemplate> templates = new ArrayList<>();
        for (String internalFileName : FileTemplateRegistratorImpl.last().getInternalTemplates().keySet()) {
            templates.add(getInternalTemplate(internalFileName));
        }
        return templates.toArray(FileTemplate.EMPTY_ARRAY);
    }

    @Override
    public FileTemplate getInternalTemplate(String templateName) {
        FileTemplateBase template = (FileTemplateBase) findInternalTemplate(templateName);

        if (template == null) {
            template = (FileTemplateBase) getJ2eeTemplate(templateName); // Hack to be able to register class templates from the plugin.
            if (template != null) {
                template.setReformatCode(true);
            }
            else {
                String text = normalizeText(getDefaultClassTemplateText(templateName));
                template = getSettings().getInternalTemplatesManager().addTemplate(templateName, "java");
                template.setText(text);
            }
        }
        return template;
    }

    @Override
    public FileTemplate findInternalTemplate(String templateName) {
        FileTemplateBase template = getSettings().getInternalTemplatesManager().findTemplateByName(templateName);

        if (template == null) {
            // todo: review the hack and try to get rid of this weird logic completely
            template = getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
        }
        return template;
    }

    
    public static String normalizeText(String text) {
        text = StringUtil.convertLineSeparators(text);
        text = StringUtil.replace(text, "$NAME$", "${NAME}");
        text = StringUtil.replace(text, "$PACKAGE_NAME$", "${PACKAGE_NAME}");
        text = StringUtil.replace(text, "$DATE$", "${DATE}");
        text = StringUtil.replace(text, "$TIME$", "${TIME}");
        text = StringUtil.replace(text, "$USER$", "${USER}");
        return text;
    }

    @Override
    
    public String internalTemplateToSubject(String templateName) {
        String subject = FileTemplateRegistratorImpl.last().getInternalTemplates().get(templateName);
        return subject == null ? templateName.toLowerCase() : subject;
    }

    @Override
    
    public String localizeInternalTemplateName(FileTemplate template) {
        return template.getName();
    }

    
    private String getDefaultClassTemplateText(String templateName) {
        return IdeLocalize.templateDefaultClassComment(Application.get().getName()) +
            "package $PACKAGE_NAME$;\n" +
            "public " + internalTemplateToSubject(templateName) + " $NAME$ { }";
    }

    @Override
    public FileTemplate getCodeTemplate(String templateName) {
        return getTemplateFromManager(templateName, getSettings().getCodeTemplatesManager());
    }

    @Override
    public FileTemplate getJ2eeTemplate(String templateName) {
        return getTemplateFromManager(templateName, getSettings().getJ2eeTemplatesManager());
    }

    private static @Nullable FileTemplate getTemplateFromManager(String templateName, FTManager ftManager) {
        FileTemplateBase template = ftManager.getTemplate(templateName);
        if (template != null) {
            return template;
        }
        template = ftManager.findTemplateByName(templateName);
        if (template != null) {
            return template;
        }
        if (templateName.endsWith("ForTest") && Application.get().isUnitTestMode()) {
            return null;
        }

        String message = "Template not found: " + templateName/*ftManager.templateNotFoundMessage(templateName)*/;
        LOG.error(message);
        return null;
    }

    @Override
    
    public FileTemplate getDefaultTemplate(String name) {
        String templateQName = myTypeManager.getExtension(name).isEmpty() ? FileTemplateBase.getQualifiedName(name, "java") : name;

        for (FTManager manager : getAllManagers()) {
            FileTemplateBase template = manager.getTemplate(templateQName);
            if (template instanceof BundledFileTemplate bundledFileTemplate) {
                BundledFileTemplate copy = bundledFileTemplate.clone();
                copy.revertToDefaults();
                return copy;
            }
        }

        String message = "Default template not found: " + name;
        LOG.error(message);
        return null;
    }

    @Override
    
    public FileTemplate[] getAllPatterns() {
        Collection<FileTemplateBase> allTemplates = getSettings().getPatternsManager().getAllTemplates(false);
        return allTemplates.toArray(new FileTemplate[allTemplates.size()]);
    }

    @Override
    public FileTemplate getPattern(String name) {
        return getSettings().getPatternsManager().findTemplateByName(name);
    }

    @Override
    
    public FileTemplate[] getAllCodeTemplates() {
        Collection<FileTemplateBase> templates = getSettings().getCodeTemplatesManager().getAllTemplates(false);
        return templates.toArray(new FileTemplate[templates.size()]);
    }

    @Override
    
    public FileTemplate[] getAllJ2eeTemplates() {
        Collection<FileTemplateBase> templates = getSettings().getJ2eeTemplatesManager().getAllTemplates(false);
        return templates.toArray(new FileTemplate[templates.size()]);
    }

    @Override
    public void setTemplates(String templatesCategory, Collection<FileTemplate> templates) {
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

    public @Nullable FileTemplateStreamProvider getDefaultTemplateDescription() {
        return myDefaultSettings.getDefaultTemplateDescription();
    }

    public @Nullable FileTemplateStreamProvider getDefaultIncludeDescription() {
        return myDefaultSettings.getDefaultIncludeDescription();
    }

    private Date myTestDate;

    @TestOnly
    public void setTestDate(Date testDate) {
        myTestDate = testDate;
    }

    @Override
    public @Nullable State getState() {
        myState.SCHEME = myScheme.getName();
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
        FileTemplatesScheme scheme =
            myProjectScheme != null && myProjectScheme.getName().equals(state.SCHEME) ? myProjectScheme : FileTemplatesScheme.DEFAULT;
        setScheme(scheme);
    }

    private FTManager[] getAllManagers() {
        return getSettings().getAllManagers();
    }

    public static class State {
        public List<String> RECENT_TEMPLATES = new ArrayList<>();
        public String SCHEME = FileTemplatesScheme.DEFAULT.getName();

        public void addName(String name) {
            RECENT_TEMPLATES.remove(name);
            RECENT_TEMPLATES.add(name);
        }

        
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
