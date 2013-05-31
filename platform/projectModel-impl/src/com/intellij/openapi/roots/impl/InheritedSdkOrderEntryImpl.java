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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectSdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 * @author dsl
 */
public class InheritedSdkOrderEntryImpl extends LibraryOrderEntryBaseImpl implements InheritedSdkOrderEntry, ClonableOrderEntry, WritableOrderEntry {
  @NonNls public static final String ENTRY_TYPE = JpsModuleRootModelSerializer.INHERITED_JDK_TYPE;
  private final MyJdkTableListener myJdkTableListener = new MyJdkTableListener();
  private final MyProjectJdkListener myListener = new MyProjectJdkListener();

  InheritedSdkOrderEntryImpl(RootModelImpl rootModel, ProjectRootManagerImpl projectRootManager) {
    super(rootModel, projectRootManager);
    myProjectRootManagerImpl.addProjectJdkListener(myListener);
    myProjectRootManagerImpl.addJdkTableListener(myJdkTableListener);
    init();
  }

  /**
   * @param element
   * @param rootModel
   * @param projectRootManager
   * @throws InvalidDataException
   */
  InheritedSdkOrderEntryImpl(Element element, RootModelImpl rootModel, ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
    this(rootModel, projectRootManager);
    if (!element.getName().equals(OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME)) {
      throw new InvalidDataException(element.getName());
    }
  }

  @Override
  public OrderEntry cloneEntry(RootModelImpl rootModel,
                               ProjectRootManagerImpl projectRootManager,
                               VirtualFilePointerManager filePointerManager) {
    return new InheritedSdkOrderEntryImpl(rootModel, projectRootManager);
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public boolean isValid() {
    return !isDisposed() && getSdk() != null;
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, R initialValue) {
    return policy.visitInheritedJdkOrderEntry(this, initialValue);
  }

  @Override
  public void writeExternal(Element rootElement) throws WriteExternalException {
    final Element orderEntryElement = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
    rootElement.addContent(orderEntryElement);
  }

  @Override
  public Sdk getSdk() {
    final Project project = getRootModel().getModule().getProject();
    return getRootModel().getConfigurationAccessor().getProjectSdk(project);
  }

  @Override
  public String getSdkName() {
    final Project project = getRootModel().getModule().getProject();
    return getRootModel().getConfigurationAccessor().getProjectSdkName(project);
  }

  @Override
  protected RootProvider getRootProvider() {
    final Sdk projectJdk = myProjectRootManagerImpl.getProjectSdk();
    return projectJdk == null ? null : projectJdk.getRootProvider();
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return "< " + getSdkName() + " >";
  }

  @Override
  public void dispose() {
    super.dispose();
    myProjectRootManagerImpl.removeJdkTableListener(myJdkTableListener);
    myProjectRootManagerImpl.removeProjectJdkListener(myListener);
  }


  private class MyJdkTableListener implements ProjectSdkTable.Listener {
    @Override
    public void sdkRemoved(Sdk jdk) {
      if (jdk.equals(getSdk())) {
        updateFromRootProviderAndSubscribe();
      }
    }

    @Override
    public void sdkAdded(Sdk jdk) {
      if (isAffectedByJdk(jdk)) {
        updateFromRootProviderAndSubscribe();
      }
    }

    @Override
    public void sdkNameChanged(Sdk jdk, String previousName) {
      if (isAffectedByJdk(jdk)) {
        // if current name matches my name
        updateFromRootProviderAndSubscribe();
      }
    }

    private boolean isAffectedByJdk(Sdk jdk) {
      return jdk.getName().equals(getSdkName());
    }
  }

  private class MyProjectJdkListener implements ProjectRootManagerEx.ProjectJdkListener {
    @Override
    public void projectJdkChanged() {
      updateFromRootProviderAndSubscribe();
    }
  }


}
