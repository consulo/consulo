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
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.extensions.PluginAware;
import consulo.logging.Logger;

import javax.annotation.Nullable;

/**
 * @author yole
 */
public class ChangesViewContentEP implements PluginAware {
  private static final Logger LOG = Logger.getInstance(ChangesViewContentEP.class);

  public static final ExtensionPointName<ChangesViewContentEP> EP_NAME = ExtensionPointName.create("com.intellij.changesViewContent");

  @Attribute("tabName") public String tabName;

  @Attribute("className") public String className;

  @Attribute("predicateClassName") public String predicateClassName;

  private PluginDescriptor myPluginDescriptor;

  private ChangesViewContentProvider myInstance;

  @Override
  public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }

  public String getTabName() {
    return tabName;
  }

  public void setTabName(final String tabName) {
    this.tabName = tabName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(final String className) {
    this.className = className;
  }

  public String getPredicateClassName() {
    return predicateClassName;
  }

  public void setPredicateClassName(final String predicateClassName) {
    this.predicateClassName = predicateClassName;
  }

  public ChangesViewContentProvider getInstance(Project project) {
    if (myInstance == null) {
      myInstance = newClassInstance(project, className);
    }
    return myInstance;
  }

  @Nullable
  public NotNullFunction<Project, Boolean> newPredicateInstance(Project project) {
    return predicateClassName != null ? newClassInstance(project, predicateClassName) : null;
  }

  @Nullable
  private <T> T newClassInstance(final Project project, final String className) {
    Class<T> resolvedClass = PluginManager.resolveClass(className, myPluginDescriptor);
    if (resolvedClass == null) {
      LOG.error("Can't resolve class by name: " + className + " plugin: " + myPluginDescriptor);
      return null;
    }
    return project.getInjectingContainer().getUnbindedInstance(resolvedClass);
  }
}
