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
package consulo.module.impl.internal.layer;

import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.extension.ModuleExtension;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author nik
 */
public abstract class RootModelBase implements ModuleRootModel {
  @Override
  
  public VirtualFile[] getContentRoots() {
    return getCurrentLayer().getContentRoots();
  }

  @Override
  
  public String[] getContentRootUrls() {
    return getCurrentLayer().getContentRootUrls();
  }

  @Override
  
  public String[] getExcludeRootUrls() {
    return getCurrentLayer().getExcludeRootUrls();
  }

  @Override
  
  public VirtualFile[] getExcludeRoots() {
    return getCurrentLayer().getExcludeRoots();
  }

  @Override
  public boolean iterateContentEntries(Predicate<ContentEntry> processor) {
    return getCurrentLayer().iterateContentEntries(processor);
  }

  
  @Override
  public String[] getContentFolderUrls(Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolderUrls(predicate);
  }

  
  @Override
  public VirtualFile[] getContentFolderFiles(Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolderFiles(predicate);
  }

  
  @Override
  public ContentFolder[] getContentFolders(Predicate<ContentFolderTypeProvider> predicate) {
    return getCurrentLayer().getContentFolders(predicate);
  }

  @Override
  
  public String[] getSourceRootUrls() {
    return getCurrentLayer().getSourceRootUrls();
  }

  @Override
  
  public String[] getSourceRootUrls(boolean includingTests) {
    return getCurrentLayer().getSourceRootUrls(includingTests);
  }

  @Override
  
  public VirtualFile[] getSourceRoots() {
    return getCurrentLayer().getSourceRoots();
  }

  @Override
  
  public VirtualFile[] getSourceRoots(boolean includingTests) {
    return getCurrentLayer().getSourceRoots(includingTests);
  }

  @Override
  public ContentEntry[] getContentEntries() {
    return getCurrentLayer().getContentEntries();
  }

  
  @Override
  public OrderEnumerator orderEntries() {
    return getCurrentLayer().orderEntries();
  }

  @Override
  public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
    return getCurrentLayer().processOrder(policy, initialValue);
  }

  @Override
  
  public String[] getDependencyModuleNames() {
    return getCurrentLayer().getDependencyModuleNames();
  }

  @Override
  
  public Module[] getModuleDependencies() {
   return getCurrentLayer().getModuleDependencies();
  }

  @Override
  
  public Module[] getModuleDependencies(boolean includeTests) {
    return getCurrentLayer().getModuleDependencies(includeTests);
  }

  @Override
  
  public OrderEntry[] getOrderEntries() {
    return getCurrentLayer().getOrderEntries();
  }

  @Override
  public <T extends ModuleExtension> @Nullable T getExtension(Class<T> clazz) {
    return getCurrentLayer().getExtension(clazz);
  }

  @Override
  public <T extends ModuleExtension> @Nullable T getExtension(String key) {
    return getCurrentLayer().getExtension(key);
  }

  @Override
  public <T extends ModuleExtension> @Nullable T getExtensionWithoutCheck(Class<T> clazz) {
    return getCurrentLayer().getExtensionWithoutCheck(clazz);
  }

  @Override
  public <T extends ModuleExtension> @Nullable T getExtensionWithoutCheck(String key) {
    return getCurrentLayer().getExtensionWithoutCheck(key);
  }

  
  @Override
  public List<ModuleExtension> getExtensions() {
    return getCurrentLayer().getExtensions();
  }
}
