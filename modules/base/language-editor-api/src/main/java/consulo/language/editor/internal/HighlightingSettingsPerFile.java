/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.editor.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.language.editor.DefaultHighlightingSettingProvider;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.highlight.FileHighlightingSettingListener;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.content.scope.ProjectScopes;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@State(name = "HighlightingSettingsPerFile", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class HighlightingSettingsPerFile extends HighlightingLevelManager implements PersistentStateComponent<Element> {
    private static final String SETTING_TAG = "setting";
    private static final String ROOT_ATT_PREFIX = "root";
    private static final String FILE_ATT = "file";

    public static HighlightingSettingsPerFile getInstance(Project project) {
        return (HighlightingSettingsPerFile)project.getInstance(HighlightingLevelManager.class);
    }

    private final Map<VirtualFile, FileHighlightingSetting[]> myHighlightSettings = new HashMap<>();

    private final MessageBus myBus;

    @Inject
    public HighlightingSettingsPerFile(@Nonnull Project project) {
        myBus = project.getMessageBus();
    }

    @Nonnull
    public FileHighlightingSetting getHighlightingSettingForRoot(@Nonnull PsiElement root) {
        PsiFile containingFile = root.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        FileHighlightingSetting[] fileHighlightingSettings = myHighlightSettings.get(virtualFile);
        int index = PsiUtilBase.getRootIndex(root);

        if (fileHighlightingSettings == null || fileHighlightingSettings.length <= index) {
            return getDefaultHighlightingSetting(root.getProject(), virtualFile);
        }
        return fileHighlightingSettings[index];
    }

    @Nonnull
    private static FileHighlightingSetting getDefaultHighlightingSetting(@Nonnull Project project, VirtualFile virtualFile) {
        if (virtualFile != null) {
            List<DefaultHighlightingSettingProvider> filtered =
                DumbService.getInstance(project).filterByDumbAwareness(DefaultHighlightingSettingProvider.EP_NAME.getExtensionList());
            for (DefaultHighlightingSettingProvider p : filtered) {
                FileHighlightingSetting setting = p.getDefaultSetting(project, virtualFile);
                if (setting != null) {
                    return setting;
                }
            }
        }
        return FileHighlightingSetting.FORCE_HIGHLIGHTING;
    }

    @Nonnull
    private static FileHighlightingSetting[] getDefaults(@Nonnull PsiFile file) {
        int rootsCount = file.getViewProvider().getLanguages().size();
        FileHighlightingSetting[] fileHighlightingSettings = new FileHighlightingSetting[rootsCount];
        for (int i = 0; i < fileHighlightingSettings.length; i++) {
            fileHighlightingSettings[i] = FileHighlightingSetting.FORCE_HIGHLIGHTING;
        }
        return fileHighlightingSettings;
    }

    public void setHighlightingSettingForRoot(@Nonnull PsiElement root, @Nonnull FileHighlightingSetting setting) {
        PsiFile containingFile = root.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return;
        }
        FileHighlightingSetting[] defaults = myHighlightSettings.get(virtualFile);
        int rootIndex = PsiUtilBase.getRootIndex(root);
        if (defaults != null && rootIndex >= defaults.length) {
            defaults = null;
        }
        if (defaults == null) {
            defaults = getDefaults(containingFile);
        }
        defaults[rootIndex] = setting;
        boolean toRemove = true;
        for (FileHighlightingSetting aDefault : defaults) {
            if (aDefault != FileHighlightingSetting.NONE) {
                toRemove = false;
            }
        }
        if (toRemove) {
            myHighlightSettings.remove(virtualFile);
        }
        else {
            myHighlightSettings.put(virtualFile, defaults);
        }

        myBus.syncPublisher(FileHighlightingSettingListener.class).settingChanged(root, setting);
    }

    @Override
    public void loadState(Element element) {
        List children = element.getChildren(SETTING_TAG);
        for (Object aChildren : children) {
            Element child = (Element)aChildren;
            String url = child.getAttributeValue(FILE_ATT);
            if (url == null) {
                continue;
            }
            VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(url);
            if (fileByUrl != null) {
                List<FileHighlightingSetting> settings = new ArrayList<>();
                int index = 0;
                while (child.getAttributeValue(ROOT_ATT_PREFIX + index) != null) {
                    String attributeValue = child.getAttributeValue(ROOT_ATT_PREFIX + index++);
                    settings.add(Enum.valueOf(FileHighlightingSetting.class, attributeValue));
                }
                myHighlightSettings.put(fileByUrl, settings.toArray(new FileHighlightingSetting[settings.size()]));
            }
        }
    }

    @Override
    public Element getState() {
        Element element = new Element("state");
        for (Map.Entry<VirtualFile, FileHighlightingSetting[]> entry : myHighlightSettings.entrySet()) {
            Element child = new Element(SETTING_TAG);

            VirtualFile vFile = entry.getKey();
            if (!vFile.isValid()) {
                continue;
            }
            child.setAttribute(FILE_ATT, vFile.getUrl());
            for (int i = 0; i < entry.getValue().length; i++) {
                FileHighlightingSetting fileHighlightingSetting = entry.getValue()[i];
                child.setAttribute(ROOT_ATT_PREFIX + i, fileHighlightingSetting.toString());
            }
            element.addContent(child);
        }
        return element;
    }

    @Override
    public boolean shouldHighlight(@Nonnull PsiElement psiRoot) {
        FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
        return settingForRoot != FileHighlightingSetting.SKIP_HIGHLIGHTING;
    }

    @Override
    public boolean shouldInspect(@Nonnull PsiElement psiRoot) {
        if (Application.get().isUnitTestMode()) {
            return true;
        }

        if (!shouldHighlight(psiRoot)) {
            return false;
        }
        Project project = psiRoot.getProject();
        VirtualFile virtualFile = psiRoot.getContainingFile().getVirtualFile();
        if (virtualFile == null || !virtualFile.isValid()) {
            return false;
        }

        if (ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile)) {
            return false;
        }

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (ProjectScopes.getLibrariesScope(project).contains(virtualFile) && !fileIndex.isInContent(virtualFile)) {
            return false;
        }

        if (RawFileLoaderHelper.isTooLargeForIntelligence(virtualFile)) {
            return false;
        }

        FileHighlightingSetting settingForRoot = getHighlightingSettingForRoot(psiRoot);
        return settingForRoot != FileHighlightingSetting.SKIP_INSPECTION;
    }
}
