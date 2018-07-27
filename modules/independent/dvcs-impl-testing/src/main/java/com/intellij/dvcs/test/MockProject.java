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
package com.intellij.dvcs.test;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;

import javax.annotation.Nonnull;

/**
 * 
 * @author Kirill Likhodedov
 */
public class MockProject implements Project {

  private final String myProjectDir;

  public MockProject(String projectDir) {
    myProjectDir = projectDir;
  }

  @Nonnull
  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile getBaseDir() {
    return new MockVirtualFile(myProjectDir);
  }

  @Override
  public String getBasePath() {
    return myProjectDir;
  }

  @Override
  public VirtualFile getProjectFile() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String getProjectFilePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPresentableUrl() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String getLocationHash() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void save() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isOpen() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInitialized() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDefault() {
    return false;
  }

  @Override
  public <T> T getComponent(Class<T> interfaceClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getComponent(Class<T> interfaceClass, T defaultImplementationIfAbsent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasComponent(@Nonnull Class interfaceClass) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T> T[] getComponents(Class<T> baseClass) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    return null;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Nonnull
  @Override
  public <T> T[] getExtensions(ExtensionPointName<T> extensionPointName) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Condition getDisposed() {
    return Condition.FALSE;
  }

  @Override
  public void dispose() {
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, T value) {
    throw new UnsupportedOperationException();
  }
}
