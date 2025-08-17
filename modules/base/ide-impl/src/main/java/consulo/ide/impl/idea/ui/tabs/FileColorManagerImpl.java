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

package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.language.editor.FileColorManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ImmutableMapBuilder;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
@Singleton
@ServiceImpl
@State(name = "FileColors", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class FileColorManagerImpl extends FileColorManager implements PersistentStateComponent<Element> {
    @Nonnull
    private final ApplicationFileColorManager myApplicationFileColorManager;
    private final Project myProject;
    private final FileColorsModel myModel;
    private FileColorSharedConfigurationManager mySharedConfigurationManager;

    private static final Map<String, Color> ourDefaultColors = ImmutableMapBuilder.<String, Color>newBuilder()
        .put("Blue", JBColor.namedColor("FileColor.Blue", new JBColor(0xeaf6ff, 0x4f556b)))
        .put("Green", JBColor.namedColor("FileColor.Green", new JBColor(0xeffae7, 0x49544a)))
        .put("Orange", JBColor.namedColor("FileColor.Orange", new JBColor(0xf6e9dc, 0x806052)))
        .put("Rose", JBColor.namedColor("FileColor.Rose", new JBColor(0xf2dcda, 0x6e535b)))
        .put("Violet", JBColor.namedColor("FileColor.Violet", new JBColor(0xe6e0f1, 0x534a57)))
        .put("Yellow", JBColor.namedColor("FileColor.Yellow", new JBColor(0xffffe4, 0x4f4b41)))
        .build();

    @Inject
    public FileColorManagerImpl(@Nonnull ApplicationFileColorManager applicationFileColorManager, @Nonnull Project project) {
        myApplicationFileColorManager = applicationFileColorManager;
        myProject = project;
        myModel = new FileColorsModel(project);
    }

    private void initProjectLevelConfigurations() {
        if (mySharedConfigurationManager == null) {
            try {
                FileColorSharedConfigurationManager.IMPL.set(this);

                mySharedConfigurationManager = myProject.getInstance(FileColorSharedConfigurationManager.class);
            }
            finally {
                FileColorSharedConfigurationManager.IMPL.remove();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return myApplicationFileColorManager.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        myApplicationFileColorManager.setEnabled(enabled);
    }

    public void setEnabledForTabs(boolean enabled) {
        myApplicationFileColorManager.setEnabledForTabs(enabled);
    }

    @Override
    public boolean isEnabledForTabs() {
        return myApplicationFileColorManager.isEnabledForTabs();
    }

    @Override
    public boolean isEnabledForProjectView() {
        return myApplicationFileColorManager.isEnabledForProjectView();
    }

    public void setEnabledForProjectView(boolean enabled) {
        myApplicationFileColorManager.setEnabledForProjectView(enabled);
    }

    public Element getState(boolean isProjectLevel) {
        return myModel.save(isProjectLevel);
    }

    @Override
    @Nullable
    public Color getColor(@Nonnull String name) {
        Color color = ourDefaultColors.get(name);
        return color == null ? ColorUtil.fromHex(name, null) : color;
    }

    @Override
    public Element getState() {
        initProjectLevelConfigurations();
        return getState(false);
    }

    void loadState(Element state, boolean isProjectLevel) {
        myModel.load(state, isProjectLevel);
    }

    @Override
    public Collection<String> getColorNames() {
        Set<String> names = ourDefaultColors.keySet();
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        return sorted;
    }

    @Override
    public void loadState(Element state) {
        initProjectLevelConfigurations();
        loadState(state, false);
    }

    @Override
    public boolean isColored(@Nonnull String scopeName, boolean shared) {
        return myModel.isColored(scopeName, shared);
    }

    @RequiredReadAction
    @Nullable
    @Override
    public Color getRendererBackground(VirtualFile file) {
        return getRendererBackground(PsiManager.getInstance(myProject).findFile(file));
    }

    @Nullable
    @Override
    public Color getRendererBackground(PsiFile file) {
        if (file == null) {
            return null;
        }

        if (isEnabled()) {
            Color fileColor = getFileColor(file);
            if (fileColor != null) {
                return fileColor;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Color getFileColor(@Nonnull PsiFile file) {
        initProjectLevelConfigurations();

        String colorName = myModel.getColor(file);
        return colorName == null ? null : getColor(colorName);
    }

    @Override
    @Nullable
    public Color getFileColor(@Nonnull VirtualFile file) {
        initProjectLevelConfigurations();
        if (!file.isValid()) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
        if (psiFile != null) {
            return getFileColor(psiFile);
        }
        else {
            String colorName = myModel.getColor(file, getProject());
            return colorName == null ? null : getColor(colorName);
        }
    }

    @Override
    @Nullable
    public Color getScopeColor(@Nonnull String scopeName) {
        initProjectLevelConfigurations();

        String colorName = myModel.getScopeColor(scopeName, getProject());
        return colorName == null ? null : getColor(colorName);
    }

    @Override
    public boolean isShared(@Nonnull String scopeName) {
        return myModel.isProjectLevel(scopeName);
    }

    FileColorsModel getModel() {
        return myModel;
    }

    boolean isShared(FileColorConfiguration configuration) {
        return myModel.isProjectLevel(configuration);
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    public List<FileColorConfiguration> getLocalConfigurations() {
        return myModel.getProjectLevelConfigurations();
    }

    public List<FileColorConfiguration> getSharedConfigurations() {
        return myModel.getLocalConfigurations();
    }

    @Nullable
    public static String getColorName(Color color) {
        for (String name : ourDefaultColors.keySet()) {
            if (color.equals(ourDefaultColors.get(name))) {
                return name;
            }
        }
        return null;
    }

    @Nullable
    private static FileColorConfiguration findConfigurationByName(String name, List<FileColorConfiguration> configurations) {
        for (FileColorConfiguration configuration : configurations) {
            if (name.equals(configuration.getScopeName())) {
                return configuration;
            }
        }
        return null;
    }

    static String getAlias(String text) {
        if (StyleManager.get().getCurrentStyle().isDark()) {
            if (text.equals("Yellow")) {
                return "Brown";
            }
        }
        return text;
    }
}
