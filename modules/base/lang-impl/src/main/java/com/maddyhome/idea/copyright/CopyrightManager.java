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

package com.maddyhome.idea.copyright;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.util.containers.HashMap;
import com.maddyhome.idea.copyright.actions.UpdateCopyrightProcessor;
import com.maddyhome.idea.copyright.util.NewFileTracker;
import consulo.copyright.config.CopyrightFileConfig;
import consulo.copyright.config.CopyrightFileConfigManager;
import consulo.logging.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@State(name = "CopyrightManager", storages = {@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/copyright/", stateSplitter = CopyrightManager.CopyrightStateSplitter.class)})
@Singleton
public class CopyrightManager implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(CopyrightManager.class);
  @NonNls
  private static final String COPYRIGHT = "copyright";
  @NonNls
  private static final String MODULE2COPYRIGHT = "module2copyright";
  @NonNls
  private static final String ELEMENT = "element";
  @NonNls
  private static final String MODULE = "module";
  @NonNls
  private static final String DEFAULT = "default";
  private final LinkedHashMap<String, String> myModule2Copyrights = new LinkedHashMap<String, String>();
  private final Map<String, CopyrightProfile> myCopyrights = new HashMap<String, CopyrightProfile>();
  private final CopyrightFileConfigManager myCopyrightFileConfigManager = new CopyrightFileConfigManager();
  @Nullable
  private CopyrightProfile myDefaultCopyright = null;

  private Project myProject;

  @Inject
  public CopyrightManager(@Nonnull Project project,
                          @Nonnull final EditorFactory editorFactory,
                          @Nonnull final Application application,
                          @Nonnull final FileDocumentManager fileDocumentManager,
                          @Nonnull final ProjectRootManager projectRootManager,
                          @Nonnull final PsiManager psiManager,
                          @Nonnull StartupManager startupManager) {
    myProject = project;
    if (myProject.isDefault()) {
      return;
    }

    final NewFileTracker newFileTracker = NewFileTracker.getInstance();
    Disposer.register(myProject, newFileTracker::clear);
    startupManager.runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        DocumentListener listener = new DocumentAdapter() {
          @Override
          public void documentChanged(DocumentEvent e) {
            final Document document = e.getDocument();
            final VirtualFile virtualFile = fileDocumentManager.getFile(document);
            if (virtualFile == null) return;
            if (!newFileTracker.poll(virtualFile)) return;
            if (!CopyrightUpdaters.hasExtension(virtualFile)) return;
            final Module module = projectRootManager.getFileIndex().getModuleForFile(virtualFile);
            if (module == null) return;
            final PsiFile file = psiManager.findFile(virtualFile);
            if (file == null) return;
            application.invokeLater(new Runnable() {
              @Override
              public void run() {
                if (myProject.isDisposed()) return;
                if (file.isValid() && file.isWritable()) {
                  final CopyrightProfile opts = getCopyrightOptions(file);
                  if (opts != null) {
                    new UpdateCopyrightProcessor(myProject, module, file).run();
                  }
                }
              }
            }, ModalityState.NON_MODAL, myProject.getDisposed());
          }
        };
        editorFactory.getEventMulticaster().addDocumentListener(listener, myProject);
      }
    });
  }

  public static CopyrightManager getInstance(Project project) {
    return project.getComponent(CopyrightManager.class);
  }

  private void readExternal(Element element) {
    clearCopyrights();
    final Element module2copyright = element.getChild(MODULE2COPYRIGHT);
    if (module2copyright != null) {
      for (Element o : module2copyright.getChildren(ELEMENT)) {
        final String moduleName = o.getAttributeValue(MODULE);
        final String copyrightName = o.getAttributeValue(COPYRIGHT);
        myModule2Copyrights.put(moduleName, copyrightName);
      }
    }
    for (Element o : element.getChildren(COPYRIGHT)) {
      final CopyrightProfile copyrightProfile = new CopyrightProfile();
      copyrightProfile.readExternal(o);
      myCopyrights.put(copyrightProfile.getName(), copyrightProfile);
    }
    myDefaultCopyright = myCopyrights.get(element.getAttributeValue(DEFAULT));
    myCopyrightFileConfigManager.readExternal(element);
  }

  private void writeExternal(Element element) {
    for (CopyrightProfile copyright : myCopyrights.values()) {
      final Element copyrightElement = new Element(COPYRIGHT);
      copyright.writeExternal(copyrightElement);
      element.addContent(copyrightElement);
    }
    final Element map = new Element(MODULE2COPYRIGHT);
    for (String moduleName : myModule2Copyrights.keySet()) {
      final Element setting = new Element(ELEMENT);
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
    final Element e = new Element("settings");
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
      final String profileName = myModule2Copyrights.get(it.next());
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
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || myCopyrightFileConfigManager.getOptions(virtualFile.getFileType()).getFileTypeOverride() == CopyrightFileConfig.NO_COPYRIGHT) {
      return null;
    }
    final DependencyValidationManager validationManager = DependencyValidationManager.getInstance(myProject);
    for (String scopeName : myModule2Copyrights.keySet()) {
      final NamedScope namedScope = validationManager.getScope(scopeName);
      if (namedScope != null) {
        final PackageSet packageSet = namedScope.getValue();
        if (packageSet != null) {
          if (packageSet.contains(file, validationManager)) {
            final CopyrightProfile profile = myCopyrights.get(myModule2Copyrights.get(scopeName));
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

  static final class CopyrightStateSplitter extends MainConfigurationStateSplitter {
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
