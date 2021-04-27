/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.impl.ModuleConfigurationStateImpl;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.util.EventDispatcher;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.roots.ui.configuration.ExtensionEditor;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * Date: Oct 4, 2003
 * Time: 6:29:56 PM
 */
public abstract class ModuleEditor implements Disposable {
  public static final String SELECTED_EDITOR_NAME = "selectedEditor";

  private final Project myProject;
  private JPanel myGenericSettingsPanel;
  private ModifiableRootModel myModifiableRootModel; // important: in order to correctly update OrderEntries UI use corresponding proxy for the model

  private final ModulesConfigurator myModulesConfigurator;
  private final LibrariesConfigurator myLibrariesConfigurator;

  private String myName;
  private final Module myModule;

  protected final List<ModuleConfigurationEditor> myEditors = new ArrayList<>();
  private ModifiableRootModel myModifiableRootModelProxy;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);
  private static final String METHOD_COMMIT = "commit";

  public ModuleEditor(Project project, ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator, @Nonnull Module module) {
    myProject = project;
    myModulesConfigurator = modulesConfigurator;
    myLibrariesConfigurator = librariesConfigurator;
    myModule = module;
    myName = module.getName();
  }

  protected abstract JComponent createCenterPanel(Disposable parentUIDisposable);

  @Nullable
  public abstract ModuleConfigurationEditor getSelectedEditor();

  public abstract void selectEditor(String displayName);

  protected abstract void restoreSelectedEditor();

  @Nullable
  public abstract ModuleConfigurationEditor getEditor(@Nonnull String displayName);

  protected abstract void disposeCenterPanel();

  public interface ChangeListener extends EventListener {
    void moduleStateChanged(ModifiableRootModel moduleRootModel);
  }

  public void addChangeListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  @Nullable
  public Module getModule() {
    final Module[] all = myModulesConfigurator.getModules();
    for (Module each : all) {
      if (each == myModule) return myModule;
    }

    return myModulesConfigurator.getModule(myName);
  }

  @RequiredReadAction
  public ModifiableRootModel getModifiableRootModel() {
    if (myModifiableRootModel == null) {
      final Module module = getModule();
      if (module != null) {
        myModifiableRootModel = ((ModuleRootManagerImpl)ModuleRootManager.getInstance(module)).getModifiableModel(new UIRootConfigurationAccessor(myModulesConfigurator, myLibrariesConfigurator));
      }
    }
    return myModifiableRootModel;
  }

  public OrderEntry[] getOrderEntries() {
    if (myModifiableRootModel == null) { // do not clone all model if not necessary
      return ModuleRootManager.getInstance(getModule()).getOrderEntries();
    }
    else {
      return myModifiableRootModel.getOrderEntries();
    }
  }

  public ModifiableRootModel getModifiableRootModelProxy() {
    if (myModifiableRootModelProxy == null) {
      final ModifiableRootModel rootModel = getModifiableRootModel();
      if (rootModel != null) {
        myModifiableRootModelProxy =
                (ModifiableRootModel)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ModifiableRootModel.class}, new ModifiableRootModelInvocationHandler(rootModel));
      }
    }
    return myModifiableRootModelProxy;
  }

  public ModuleRootModel getRootModel() {
    if (myModifiableRootModel != null) {
      return getModifiableRootModelProxy();
    }
    return ModuleRootManager.getInstance(myModule);
  }

  public boolean isModified() {
    for (ModuleConfigurationEditor moduleElementsEditor : myEditors) {
      if (moduleElementsEditor.isModified()) {
        return true;
      }
    }
    return false;
  }

  private void createEditors() {
    ModuleConfigurationState state = createModuleConfigurationState();
    ContentEntriesEditor contentEntriesEditor = new ContentEntriesEditor(myName, state);
    myEditors.add(contentEntriesEditor);
    CompilerOutputsEditor compilerOutputEditor = new CompilerOutputsEditor(state);
    myEditors.add(compilerOutputEditor);
    final ClasspathEditor classpathEditor = new ClasspathEditor(state);
    myEditors.add(classpathEditor);
    myEditors.add(new ExtensionEditor(state, compilerOutputEditor, classpathEditor, contentEntriesEditor));
  }

  public ModuleConfigurationState createModuleConfigurationState() {
    return new ModuleConfigurationStateImpl(myProject, myModulesConfigurator, myLibrariesConfigurator, this::getModifiableRootModelProxy);
  }

  private JPanel createPanel(@Nonnull Disposable parentUIDisposable) {
    getModifiableRootModel(); //initialize model if needed
    getModifiableRootModelProxy();

    myGenericSettingsPanel = new ModuleEditorPanel();

    createEditors();

    JPanel northPanel = new JPanel(new GridBagLayout());

    myGenericSettingsPanel.add(northPanel, BorderLayout.NORTH);

    final JComponent component = createCenterPanel(parentUIDisposable);
    myGenericSettingsPanel.add(component, BorderLayout.CENTER);
    return myGenericSettingsPanel;
  }

  public JPanel getPanel(Disposable parentUIDisposable) {
    if (myGenericSettingsPanel == null) {
      myGenericSettingsPanel = createPanel(parentUIDisposable);
    }

    return myGenericSettingsPanel;
  }

  public void fireModuleStateChanged() {
    if (getModule() != null) { //module with attached module libraries was deleted
      //getPanel(parentUIDisposable);  //init editor if needed
      for (final ModuleConfigurationEditor myEditor : myEditors) {
        myEditor.moduleStateChanged();
      }
      myEventDispatcher.getMulticaster().moduleStateChanged(getModifiableRootModelProxy());
    }
  }

  public void updateCompilerOutputPathChanged(String baseUrl, String moduleName) {
    if (myGenericSettingsPanel == null) return; //wasn't initialized yet
    for (final ModuleConfigurationEditor myEditor : myEditors) {
      if (myEditor instanceof ModuleElementsEditor) {
        ((ModuleElementsEditor)myEditor).moduleCompileOutputChanged(baseUrl, moduleName);
      }
    }
  }

  @Override
  public void dispose() {
    try {
      for (final ModuleConfigurationEditor myEditor : myEditors) {
        myEditor.disposeUIResources();
      }

      myEditors.clear();

      disposeCenterPanel();

      if (myModifiableRootModel != null) {
        myModifiableRootModel.dispose();
      }

      myGenericSettingsPanel = null;
    }
    finally {
      myModifiableRootModel = null;
      myModifiableRootModelProxy = null;
    }
  }

  public ModifiableRootModel apply() throws ConfigurationException {
    try {
      for (ModuleConfigurationEditor editor : myEditors) {
        editor.saveData();
        editor.apply();
      }

      return myModifiableRootModel;
    }
    finally {
      myModifiableRootModel = null;
      myModifiableRootModelProxy = null;
    }
  }

  public void canApply() throws ConfigurationException {
    for (ModuleConfigurationEditor editor : myEditors) {
      if (editor instanceof ModuleElementsEditor) {
        ((ModuleElementsEditor)editor).canApply();
      }
    }
  }

  public String getName() {
    return myName;
  }

  private class ModifiableRootModelInvocationHandler implements InvocationHandler {
    private final ModifiableRootModel myDelegateModel;
    private final Set<String> myCheckedNames = new HashSet<>(
            Arrays.asList("addOrderEntry", "addLibraryEntry", "addInvalidLibrary", "addModuleOrderEntry", "addInvalidModuleEntry", "addContentEntry", "removeContentEntry", "removeOrderEntry",
                          "addModuleExtensionSdkEntry", "addLayer", "removeLayer", "setCurrentLayer", "replaceEntryOfType"));

    ModifiableRootModelInvocationHandler(ModifiableRootModel model) {
      myDelegateModel = model;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] params) throws Throwable {
      final boolean needUpdate = myCheckedNames.contains(method.getName());
      try {
        final Object result = method.invoke(myDelegateModel, unwrapParams(params));
        if (result instanceof LibraryTable) {
          return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{LibraryTable.class}, new LibraryTableInvocationHandler((LibraryTable)result));
        }
        return result;
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
      finally {
        if (needUpdate) {
          fireModuleStateChanged();
        }
      }
    }
  }

  private class LibraryTableInvocationHandler implements InvocationHandler, ProxyDelegateAccessor {
    private final LibraryTable myDelegateTable;
    private final Set<String> myCheckedNames = new HashSet<>(Arrays.asList("removeLibrary" /*,"createLibrary"*/));

    LibraryTableInvocationHandler(LibraryTable table) {
      myDelegateTable = table;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] params) throws Throwable {
      final boolean needUpdate = myCheckedNames.contains(method.getName());
      try {
        final Object result = method.invoke(myDelegateTable, unwrapParams(params));
        if (result instanceof Library) {
          return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{result instanceof LibraryEx ? LibraryEx.class : Library.class}, new LibraryInvocationHandler((Library)result));
        }
        else if (result instanceof LibraryTable.ModifiableModel) {
          return Proxy
                  .newProxyInstance(getClass().getClassLoader(), new Class[]{LibraryTableBase.ModifiableModelEx.class}, new LibraryTableModelInvocationHandler((LibraryTable.ModifiableModel)result));
        }
        if (result instanceof Library[]) {
          Library[] libraries = (Library[])result;
          for (int idx = 0; idx < libraries.length; idx++) {
            Library library = libraries[idx];
            libraries[idx] =
                    (Library)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{library instanceof LibraryEx ? LibraryEx.class : Library.class}, new LibraryInvocationHandler(library));
          }
        }
        return result;
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
      finally {
        if (needUpdate) {
          fireModuleStateChanged();
        }
      }
    }

    @Override
    public Object getDelegate() {
      return myDelegateTable;
    }
  }

  private class LibraryInvocationHandler implements InvocationHandler, ProxyDelegateAccessor {
    private final Library myDelegateLibrary;

    LibraryInvocationHandler(Library delegateLibrary) {
      myDelegateLibrary = delegateLibrary;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] params) throws Throwable {
      try {
        final Object result = method.invoke(myDelegateLibrary, unwrapParams(params));
        if (result instanceof LibraryEx.ModifiableModelEx) {
          return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{LibraryEx.ModifiableModelEx.class}, new LibraryModifiableModelInvocationHandler((LibraryEx.ModifiableModelEx)result));
        }
        return result;
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }

    @Override
    public Object getDelegate() {
      return myDelegateLibrary;
    }
  }

  private class LibraryModifiableModelInvocationHandler implements InvocationHandler, ProxyDelegateAccessor {
    private final Library.ModifiableModel myDelegateModel;

    LibraryModifiableModelInvocationHandler(Library.ModifiableModel delegateModel) {
      myDelegateModel = delegateModel;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] params) throws Throwable {
      final boolean needUpdate = METHOD_COMMIT.equals(method.getName());
      try {
        return method.invoke(myDelegateModel, unwrapParams(params));
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
      finally {
        if (needUpdate) {
          fireModuleStateChanged();
        }
      }
    }

    @Override
    public Object getDelegate() {
      return myDelegateModel;
    }
  }

  private class LibraryTableModelInvocationHandler implements InvocationHandler, ProxyDelegateAccessor {
    private final LibraryTable.ModifiableModel myDelegateModel;

    LibraryTableModelInvocationHandler(LibraryTable.ModifiableModel delegateModel) {
      myDelegateModel = delegateModel;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] params) throws Throwable {
      final boolean needUpdate = METHOD_COMMIT.equals(method.getName());
      try {
        Object result = method.invoke(myDelegateModel, unwrapParams(params));
        if (result instanceof Library[]) {
          Library[] libraries = (Library[])result;
          for (int idx = 0; idx < libraries.length; idx++) {
            Library library = libraries[idx];
            libraries[idx] = (Library)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{LibraryEx.class}, new LibraryInvocationHandler(library));
          }
        }
        if (result instanceof Library) {
          result = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{LibraryEx.class}, new LibraryInvocationHandler((Library)result));
        }
        return result;
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
      finally {
        if (needUpdate) {
          fireModuleStateChanged();
        }
      }
    }

    @Override
    public Object getDelegate() {
      return myDelegateModel;
    }
  }

  public interface ProxyDelegateAccessor {
    Object getDelegate();
  }

  private static Object[] unwrapParams(Object[] params) {
    if (params == null || params.length == 0) {
      return params;
    }
    final Object[] unwrappedParams = new Object[params.length];
    for (int idx = 0; idx < params.length; idx++) {
      Object param = params[idx];
      if (param != null && Proxy.isProxyClass(param.getClass())) {
        final InvocationHandler invocationHandler = Proxy.getInvocationHandler(param);
        if (invocationHandler instanceof ProxyDelegateAccessor) {
          param = ((ProxyDelegateAccessor)invocationHandler).getDelegate();
        }
      }
      unwrappedParams[idx] = param;
    }
    return unwrappedParams;
  }

  @Nullable
  public String getHelpTopic() {
    if (myEditors.isEmpty()) {
      return null;
    }
    final ModuleConfigurationEditor selectedEditor = getSelectedEditor();
    return selectedEditor != null ? selectedEditor.getHelpTopic() : null;
  }

  public void setModuleName(final String name) {
    myName = name;
  }

  private class ModuleEditorPanel extends JPanel implements DataProvider {
    public ModuleEditorPanel() {
      super(new BorderLayout());
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (LangDataKeys.MODULE_CONTEXT == dataId) {
        return getModule();
      }
      return null;
    }

  }
}
