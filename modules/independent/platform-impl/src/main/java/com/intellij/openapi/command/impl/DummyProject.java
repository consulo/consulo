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
package com.intellij.openapi.command.impl;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class DummyProject extends UserDataHolderBase implements Project {

  private static class DummyProjectHolder {
    private static final DummyProject ourInstance = new DummyProject();
  }

  @Nonnull
  public static Project getInstance() {
    return DummyProjectHolder.ourInstance;
  }

  private DummyProject() {
  }

  @Override
  public VirtualFile getProjectFile() {
    return null;
  }

  @Override
  @Nonnull
  public String getName() {
    return "";
  }

  @Override
  @Nullable
  @NonNls
  public String getPresentableUrl() {
    return null;
  }

  @Override
  @Nonnull
  @NonNls
  public String getLocationHash() {
    return "dummy";
  }

  @Override
  @Nonnull
  public String getProjectFilePath() {
    return "";
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    return null;
  }

  @Override
  @Nullable
  public VirtualFile getBaseDir() {
    return null;
  }

  @Override
  public String getBasePath() {
    return null;
  }

  @Override
  public void save() {
  }

  @Override
  public <T> T getComponent(Class<T> interfaceClass) {
    return null;
  }

  @Override
  public boolean hasComponent(@Nonnull Class interfaceClass) {
    return false;
  }

  @Override
  @Nonnull
  public <T> T[] getComponents(Class<T> baseClass) {
    return (T[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public <T> T getComponent(Class<T> interfaceClass, T defaultImplementation) {
    return null;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  @Nonnull
  public Condition getDisposed() {
    return new Condition() {
      @Override
      public boolean value(final Object o) {
        return isDisposed();
      }
    };
  }

  @Nonnull
  public ComponentConfig[] getComponentConfigurations() {
    return new ComponentConfig[0];
  }

  @Nullable
  public Object getComponent(final ComponentConfig componentConfig) {
    return null;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isInitialized() {
    return false;
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    return null;
  }

  @Override
  public void dispose() {
  }

  @Nonnull
  @Override
  public <T> T[] getExtensions(final ExtensionPointName<T> extensionPointName) {
    throw new UnsupportedOperationException("getExtensions()");
  }

  public ComponentConfig getConfig(Class componentImplementation) {
    throw new UnsupportedOperationException("Method getConfig not implemented in " + getClass());
  }
}
