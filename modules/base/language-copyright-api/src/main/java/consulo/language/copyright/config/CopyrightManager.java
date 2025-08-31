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

package consulo.language.copyright.config;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.*;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

@State(name = "CopyrightManager", storages = {@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/copyright/", stateSplitter = CopyrightManager.CopyrightStateSplitter.class)})
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class CopyrightManager implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(CopyrightManager.class);
  private static final String COPYRIGHT = "copyright";
  private static final String MODULE2COPYRIGHT = "module2copyright";
  private static final String ELEMENT = "element";
  private static final String MODULE = "module";
  private static final String DEFAULT = "default";

  private final LinkedHashMap<String, String> myModule2Copyrights = new LinkedHashMap<>();
  private final Map<String, CopyrightProfile> myCopyrights = new HashMap<>();
  private final CopyrightFileConfigManager myCopyrightFileConfigManager = new CopyrightFileConfigManager();
  @Nullable
  private CopyrightProfile myDefaultCopyright = null;

  private Project myProject;

  @Inject
  public CopyrightManager(@Nonnull Project project) {
    myProject = project;
  }

  public static CopyrightManager getInstance(Project project) {
    return project.getComponent(CopyrightManager.class);
  }

  private void readExternal(Element element) {
    clearCopyrights();
    Element module2copyright = element.getChild(MODULE2COPYRIGHT);
    if (module2copyright != null) {
      for (Element o : module2copyright.getChildren(ELEMENT)) {
        String moduleName = o.getAttributeValue(MODULE);
        String copyrightName = o.getAttributeValue(COPYRIGHT);
        myModule2Copyrights.put(moduleName, copyrightName);
      }
    }
    for (Element o : element.getChildren(COPYRIGHT)) {
      CopyrightProfile copyrightProfile = new CopyrightProfile();
      copyrightProfile.readExternal(o);
      myCopyrights.put(copyrightProfile.getName(), copyrightProfile);
    }
    myDefaultCopyright = myCopyrights.get(element.getAttributeValue(DEFAULT));
    myCopyrightFileConfigManager.readExternal(element);
  }

  private void writeExternal(Element element) {
    for (CopyrightProfile copyright : myCopyrights.values()) {
      Element copyrightElement = new Element(COPYRIGHT);
      copyright.writeExternal(copyrightElement);
      element.addContent(copyrightElement);
    }
    Element map = new Element(MODULE2COPYRIGHT);
    for (String moduleName : myModule2Copyrights.keySet()) {
      Element setting = new Element(ELEMENT);
      setting.setAttribute(MODULE, moduleName);
      setting.setAttribute(COPYRIGHT, myModule2Copyrights.get(moduleName));
      map.addContent(setting);
    }
    element.addContent(map);
    element.setAttribute(DEFAULT, myDefaultCopyright != null ? myDefaultCopyright.getName() : "");
    myCopyrightFileConfigManager.writeExternal(element);
  }

  @Override
  public Element getState() {
    Element e = new Element("settings");
    writeExternal(e);
    return e;
  }

  @Override
  public void loadState(Element state) {
    readExternal(state);
  }

  public Map<String, String> getCopyrightsMapping() {
    return myModule2Copyrights;
  }

  @Nullable
  public CopyrightProfile getDefaultCopyright() {
    return myDefaultCopyright;
  }

  public void setDefaultCopyright(@Nullable CopyrightProfile copyright) {
    myDefaultCopyright = copyright;
  }

  public void addCopyright(CopyrightProfile copyrightProfile) {
    myCopyrights.put(copyrightProfile.getName(), copyrightProfile);
  }

  public void removeCopyright(CopyrightProfile copyrightProfile) {
    myCopyrights.values().remove(copyrightProfile);
    for (Iterator<String> it = myModule2Copyrights.keySet().iterator(); it.hasNext(); ) {
      String profileName = myModule2Copyrights.get(it.next());
      if (profileName.equals(copyrightProfile.getName())) {
        it.remove();
      }
    }
  }

  public void clearCopyrights() {
    myDefaultCopyright = null;
    myCopyrights.clear();
    myModule2Copyrights.clear();
  }

  public void mapCopyright(String scopeName, String copyrightProfileName) {
    myModule2Copyrights.put(scopeName, copyrightProfileName);
  }

  public void unmapCopyright(String scopeName) {
    myModule2Copyrights.remove(scopeName);
  }

  public Collection<CopyrightProfile> getCopyrights() {
    return myCopyrights.values();
  }

  public boolean hasAnyCopyrights() {
    return myDefaultCopyright != null || !myModule2Copyrights.isEmpty();
  }

  @Nullable
  public CopyrightProfile getCopyrightOptions(@Nonnull PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || myCopyrightFileConfigManager.getOptions(virtualFile.getFileType()).getFileTypeOverride() == CopyrightFileConfig.NO_COPYRIGHT) {
      return null;
    }
    for (String scopeName : myModule2Copyrights.keySet()) {
      Pair<NamedScopesHolder, NamedScope> scopeWithHolder = NamedScopesHolder.getScopeWithHolder(myProject, scopeName);
      if (scopeWithHolder != null) {
        PackageSet packageSet = scopeWithHolder.getSecond().getValue();
        if (packageSet != null) {
          if (packageSet.contains(file.getVirtualFile(), file.getProject(), scopeWithHolder.getFirst())) {
            CopyrightProfile profile = myCopyrights.get(myModule2Copyrights.get(scopeName));
            if (profile != null) {
              return profile;
            }
          }
        }
      }
    }
    return myDefaultCopyright != null ? myDefaultCopyright : null;
  }

  public CopyrightFileConfigManager getCopyrightFileConfigManager() {
    return myCopyrightFileConfigManager;
  }

  public void replaceCopyright(String displayName, CopyrightProfile copyrightProfile) {
    if (myDefaultCopyright != null && Comparing.strEqual(myDefaultCopyright.getName(), displayName)) {
      myDefaultCopyright = copyrightProfile;
    }
    myCopyrights.remove(displayName);
    addCopyright(copyrightProfile);
  }

  public static final class CopyrightStateSplitter extends MainConfigurationStateSplitter {
    @Nonnull
    @Override
    protected String getSubStateFileName(@Nonnull Element element) {
      for (Element option : element.getChildren("option")) {
        if (option.getAttributeValue("name").equals("myName")) {
          return option.getAttributeValue("value");
        }
      }
      throw new IllegalStateException();
    }

    @Nonnull
    @Override
    protected String getComponentStateFileName() {
      return "profiles_settings";
    }

    @Nonnull
    @Override
    protected String getSubStateTagName() {
      return "copyright";
    }
  }
}
