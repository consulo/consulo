/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.codeStyle.impl.internal;

import consulo.component.persist.scheme.SchemeManager;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModelListener;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class CodeStyleSchemesModelImpl implements CodeStyleSchemesModel {
  private final List<CodeStyleScheme> mySchemes = new ArrayList<CodeStyleScheme>();
  private CodeStyleScheme myGlobalSelected;
  private final CodeStyleSchemeImpl myProjectScheme;
  private final CodeStyleScheme myDefault;
  private final Map<CodeStyleScheme, CodeStyleSettings> mySettingsToClone = new HashMap<CodeStyleScheme, CodeStyleSettings>();

  private final EventDispatcher<CodeStyleSchemesModelListener> myDispatcher = EventDispatcher.create(CodeStyleSchemesModelListener.class);
  private final Project myProject;
  private boolean myUsePerProjectSettings;

  public static final String PROJECT_SCHEME_NAME = "Project";

  public CodeStyleSchemesModelImpl(Project project) {
    myProject = project;
    myProjectScheme = new CodeStyleSchemeImpl(PROJECT_SCHEME_NAME, false, CodeStyleSchemes.getInstance().getDefaultScheme());
    reset();
    myDefault = CodeStyleSchemes.getInstance().getDefaultScheme();
  }

  @Override
  public void selectScheme(final CodeStyleScheme selected, @Nullable Object source) {
    if (myGlobalSelected != selected && selected != myProjectScheme) {
      myGlobalSelected = selected;
      myDispatcher.getMulticaster().currentSchemeChanged(source);
    }
  }

  @Override
  public void addScheme(final CodeStyleScheme newScheme, boolean changeSelection) {
    mySchemes.add(newScheme);
    myDispatcher.getMulticaster().schemeListChanged();
    if (changeSelection) {
      selectScheme(newScheme, this);
    }
  }

  @Override
  public void removeScheme(final CodeStyleScheme scheme) {
    mySchemes.remove(scheme);
    myDispatcher.getMulticaster().schemeListChanged();
    if (myGlobalSelected == scheme) {
      selectScheme(myDefault, this);
    }
  }

  public CodeStyleSettings getCloneSettings(final CodeStyleScheme scheme) {
    if (!mySettingsToClone.containsKey(scheme)) {
      mySettingsToClone.put(scheme, scheme.getCodeStyleSettings().clone());
    }
    return mySettingsToClone.get(scheme);
  }

  @Override
  @Nonnull
  public CodeStyleScheme getSelectedScheme(){
    if (myUsePerProjectSettings) {
      return myProjectScheme;
    }
    return myGlobalSelected;
  }

  @Override
  public void addListener(CodeStyleSchemesModelListener listener) {
    myDispatcher.addListener(listener);
  }

  public List<CodeStyleScheme> getSchemes() {
    return Collections.unmodifiableList(mySchemes);
  }

  public void reset() {
    myUsePerProjectSettings = getProjectSettings().USE_PER_PROJECT_SETTINGS;

    CodeStyleScheme[] allSchemes = CodeStyleSchemes.getInstance().getSchemes();
    mySettingsToClone.clear();
    mySchemes.clear();
    ContainerUtil.addAll(mySchemes, allSchemes);
    myGlobalSelected = CodeStyleSchemes.getInstance().findPreferredScheme(getProjectSettings().PREFERRED_PROJECT_CODE_STYLE);

    CodeStyleSettings perProjectSettings = getProjectSettings().PER_PROJECT_SETTINGS;
    if (perProjectSettings != null) {
      myProjectScheme.setCodeStyleSettings(perProjectSettings);
    }


    myDispatcher.getMulticaster().schemeListChanged();
    myDispatcher.getMulticaster().currentSchemeChanged(this);

  }

  @Override
  public boolean isUsePerProjectSettings() {
    return myUsePerProjectSettings;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  /**
   * Updates 'use per-project settings' value within the current model and optionally at the project settings.
   *
   * @param usePerProjectSettings  defines whether 'use per-project settings' are in use
   * @param commit                 flag that defines if current project settings should be applied as well
   */
  @Override
  public void setUsePerProjectSettings(final boolean usePerProjectSettings, final boolean commit) {
    if (commit) {
      final CodeStyleSettingsManager projectSettings = getProjectSettings();
      projectSettings.USE_PER_PROJECT_SETTINGS = usePerProjectSettings;
      projectSettings.PER_PROJECT_SETTINGS = myProjectScheme.getCodeStyleSettings();
    }

    if (myUsePerProjectSettings != usePerProjectSettings) {
      myUsePerProjectSettings = usePerProjectSettings;
      myDispatcher.getMulticaster().usePerProjectSettingsOptionChanged();
      myDispatcher.getMulticaster().currentSchemeChanged(this);
    }
  }

  private CodeStyleSettingsManager getProjectSettings() {
    return CodeStyleSettingsManager.getInstance(myProject);
  }

  public boolean isSchemeListModified() {
    CodeStyleSchemes schemes = CodeStyleSchemes.getInstance();
    if (getProjectSettings().USE_PER_PROJECT_SETTINGS != myUsePerProjectSettings) return true;
    if (!myUsePerProjectSettings &&
        getSelectedScheme() != schemes.findPreferredScheme(getProjectSettings().PREFERRED_PROJECT_CODE_STYLE)) {
      return true;
    }
    Set<CodeStyleScheme> configuredSchemesSet = new HashSet<CodeStyleScheme>(getSchemes());
    Set<CodeStyleScheme> savedSchemesSet = new HashSet<CodeStyleScheme>(Arrays.asList(schemes.getSchemes()));
    return !configuredSchemesSet.equals(savedSchemesSet);
  }

  public void apply() {
    CodeStyleSettingsManager projectSettingsManager = getProjectSettings();
    projectSettingsManager.USE_PER_PROJECT_SETTINGS = myUsePerProjectSettings;
    projectSettingsManager.PREFERRED_PROJECT_CODE_STYLE =
            myUsePerProjectSettings || myGlobalSelected == null ? null : myGlobalSelected.getName();
    projectSettingsManager.PER_PROJECT_SETTINGS = myProjectScheme.getCodeStyleSettings();
    final CodeStyleScheme[] savedSchemes = CodeStyleSchemes.getInstance().getSchemes();
    final Set<CodeStyleScheme> savedSchemesSet = new HashSet<CodeStyleScheme>(Arrays.asList(savedSchemes));
    List<CodeStyleScheme> configuredSchemes = getSchemes();

    for (CodeStyleScheme savedScheme : savedSchemes) {
      if (!configuredSchemes.contains(savedScheme)) {
        CodeStyleSchemes.getInstance().deleteScheme(savedScheme);
      }
    }

    for (CodeStyleScheme configuredScheme : configuredSchemes) {
      if (!savedSchemesSet.contains(configuredScheme)) {
        CodeStyleSchemes.getInstance().addScheme(configuredScheme);
      }
    }

    CodeStyleSchemes.getInstance().setCurrentScheme(myGlobalSelected);

    // We want to avoid the situation when 'real code style' differs from the copy stored here (e.g. when 'real code style' changes
    // are 'committed' by pressing 'Apply' button). So, we reset the copies here assuming that this method is called on 'Apply'
    // button processing
    mySettingsToClone.clear();

    projectSettingsManager.notifyCodeStyleSettingsChanged();
  }

  static SchemeManager<CodeStyleScheme, CodeStyleSchemeImpl> getSchemesManager() {
    return ((CodeStyleSchemesImpl) CodeStyleSchemes.getInstance()).getSchemeManager();
  }

  public static boolean cannotBeModified(final CodeStyleScheme currentScheme) {
    return currentScheme.isDefault();
  }

  public static boolean cannotBeDeleted(final CodeStyleScheme currentScheme) {
    return currentScheme.isDefault();
  }

  @Override
  public void fireCurrentSettingsChanged() {
    myDispatcher.getMulticaster().currentSettingsChanged();
  }

  @Override
  public void fireSchemeChanged(CodeStyleScheme scheme) {
    myDispatcher.getMulticaster().schemeChanged(scheme);
  }

  @Override
  public CodeStyleScheme getSelectedGlobalScheme() {
    return myGlobalSelected;
  }

  @Override
  public void copyToProject(final CodeStyleScheme selectedScheme) {
    myProjectScheme.getCodeStyleSettings().copyFrom(selectedScheme.getCodeStyleSettings());
    myDispatcher.getMulticaster().schemeChanged(myProjectScheme);
  }

  @Override
  public CodeStyleScheme exportProjectScheme(@Nonnull String name) {
    CodeStyleScheme newScheme = createNewScheme(name, myProjectScheme);
    ((CodeStyleSchemeImpl)newScheme).setCodeStyleSettings(getCloneSettings(myProjectScheme));
    addScheme(newScheme, false);

    return newScheme;
  }

  @Override
  public CodeStyleScheme createNewScheme(final String preferredName, final CodeStyleScheme parentScheme) {
    String name;
    if (preferredName == null) {
      if (parentScheme == null) throw new IllegalArgumentException("parentScheme must not be null");
      // Generate using parent name
      name = null;
      for (int i = 1; name == null; i++) {
        String currName = parentScheme.getName() + " (" + i + ")";
        if (findSchemeByName(currName) == null) {
          name = currName;
        }
      }
    }
    else {
      name = null;
      for (int i = 0; name == null; i++) {
        String currName = i == 0 ? preferredName : preferredName + " (" + i + ")";
        if (findSchemeByName(currName) == null) {
          name = currName;
        }
      }
    }

    return new CodeStyleSchemeImpl(name, false, parentScheme);
  }

  @Nullable
  private CodeStyleScheme findSchemeByName(final String name) {
    for (CodeStyleScheme scheme : mySchemes) {
      if (name.equals(scheme.getName())) return scheme;
    }
    return null;
  }

  @Override
  public CodeStyleScheme getProjectScheme() {
    return myProjectScheme;
  }

  @Override
  public boolean isProjectScheme(CodeStyleScheme scheme) {
    return myProjectScheme == scheme;
  }

  @Override
  public List<CodeStyleScheme> getAllSortedSchemes() {
    List<CodeStyleScheme> schemes = new ArrayList<CodeStyleScheme>();
    schemes.addAll(getSchemes());
    schemes.add(myProjectScheme);
    Collections.sort(schemes, (s1, s2) -> {
      if (isProjectScheme(s1)) return -1;
      if (isProjectScheme(s2)) return 1;
      if (s1.isDefault()) return -1;
      if (s2.isDefault()) return 1;
      return s1.getName().compareToIgnoreCase(s2.getName());
    });
    return schemes;
  }
}
