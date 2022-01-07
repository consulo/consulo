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
package com.intellij.openapi.roots.impl;

import com.google.common.base.Predicate;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import consulo.module.extension.ModuleExtension;
import consulo.roots.ContentFolderTypeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
public abstract class RootModelBase implements ModuleRootModel {
  @Override
  @Nonnull
  public VirtualFile[] getContentRoots() {
    return getCurrentLayer().getContentRoots();
  }

  @Override
  @Nonnull
  public String[] getContentRootUrls() {
    return getCurrentLayer().getContentRootUrls();
  }

  @Override
  @Nonnull
  public String[] getExcludeRootUrls() {
    return getCurrentLayer().getExcludeRootUrls();
  }

  @Override
  @Nonnull
  public VirtualFile[] getExcludeRoots() {
    return getCurrentLayer().getExcludeRoots();
  }

  @Override
  public boolean iterateContentEntries(@Nonnull Processor<ContentEntry> processor) {
    return getCurrentLayer().iterateContentEntries(processor);
  }

  @Nonnull
  @Override
  public String[] getContentFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolderUrls(predicate);
  }

  @Nonnull
  @Override
  public VirtualFile[] getContentFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolderFiles(predicate);
  }

  @Nonnull
  @Override
  public ContentFolder[] getContentFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolders(predicate);
  }

  @Override
  @Nonnull
  public String[] getSourceRootUrls() {
    return getCurrentLayer().getSourceRootUrls();
  }

  @Override
  @Nonnull
  public String[] getSourceRootUrls(boolean includingTests) {
    return getCurrentLayer().getSourceRootUrls(includingTests);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSourceRoots() {
    return getCurrentLayer().getSourceRoots();
  }

  @Override
  @Nonnull
  public VirtualFile[] getSourceRoots(final boolean includingTests) {
    return getCurrentLayer().getSourceRoots(includingTests);
  }

  @Override
  public ContentEntry[] getContentEntries() {
    return getCurrentLayer().getContentEntries();
  }

  @Nonnull
  @Override
  public OrderEnumerator orderEntries() {
    return getCurrentLayer().orderEntries();
  }

  @Override
  public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
    return getCurrentLayer().processOrder(policy, initialValue);
  }

  @Override
  @Nonnull
  public String[] getDependencyModuleNames() {
    return getCurrentLayer().getDependencyModuleNames();
  }

  @Override
  @Nonnull
  public Module[] getModuleDependencies() {
   return getCurrentLayer().getModuleDependencies();
  }

  @Override
  @Nonnull
  public Module[] getModuleDependencies(boolean includeTests) {
    return getCurrentLayer().getModuleDependencies(includeTests);
  }

  @Override
  @Nonnull
  public OrderEntry[] getOrderEntries() {
    return getCurrentLayer().getOrderEntries();
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtension(Class<T> clazz) {
    return getCurrentLayer().getExtension(clazz);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtension(@Nonnull String key) {
    return getCurrentLayer().getExtension(key);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz) {
    return getCurrentLayer().getExtensionWithoutCheck(clazz);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension> T getExtensionWithoutCheck(@Nonnull String key) {
    return getCurrentLayer().getExtensionWithoutCheck(key);
  }

  @Nonnull
  @Override
  public List<ModuleExtension> getExtensions() {
    return getCurrentLayer().getExtensions();
  }
}
