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
package com.intellij.psi.impl.file;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;

import java.util.*;

public abstract class PsiPackageBase extends PsiElementBase implements PsiPackage, Queryable {
  private static final Logger LOG = Logger.getInstance(PsiPackageBase.class);

  protected final PsiManager myManager;
  protected final PsiPackageManager myPackageManager;
  private final Class<? extends ModuleExtension> myExtensionClass;
  private final String myQualifiedName;

  public PsiPackageBase(PsiManager manager,
                        PsiPackageManager packageManager,
                        Class<? extends ModuleExtension> extensionClass,
                        String qualifiedName) {
    myManager = manager;
    myPackageManager = packageManager;
    myExtensionClass = extensionClass;
    myQualifiedName = qualifiedName;
  }

  protected Collection<PsiDirectory> getAllDirectories(boolean inLibrarySources) {
    List<PsiDirectory> directories = new ArrayList<PsiDirectory>();
    PsiManager manager = PsiManager.getInstance(getProject());
    Query<VirtualFile> directoriesByPackageName =
      DirectoryIndex.getInstance(getProject()).getDirectoriesByPackageName(getQualifiedName(), inLibrarySources);
    for (VirtualFile virtualFile : directoriesByPackageName) {
      PsiDirectory directory = manager.findDirectory(virtualFile);
      if (directory != null) {
        directories.add(directory);
      }
    }

    return directories;
  }

  @Override
  public boolean equals(Object o) {
    return o != null &&
           getClass() == o.getClass() &&
           myManager == ((PsiPackageBase)o).myManager &&
           myQualifiedName.equals(((PsiPackageBase)o).myQualifiedName);
  }

  @Override
  public int hashCode() {
    return myQualifiedName.hashCode();
  }

  @Nonnull
  public String getQualifiedName() {
    return myQualifiedName;
  }

  @Override
  @Nonnull
  public PsiDirectory[] getDirectories() {
    final Collection<PsiDirectory> collection = getAllDirectories(false);
    return ContainerUtil.toArray(collection, new PsiDirectory[collection.size()]);
  }

  @Override
  @Nonnull
  public PsiDirectory[] getDirectories(@Nonnull GlobalSearchScope scope) {
    List<PsiDirectory> result = null;
    final boolean includeLibrarySources = scope.isForceSearchingInLibrarySources();
    final Collection<PsiDirectory> directories = getAllDirectories(includeLibrarySources);
    for (final PsiDirectory directory : directories) {
      if (scope.contains(directory.getVirtualFile())) {
        if (result == null) result = new ArrayList<PsiDirectory>();
        result.add(directory);
      }
    }
    return result == null ? PsiDirectory.EMPTY_ARRAY : result.toArray(new PsiDirectory[result.size()]);
  }

  @RequiredReadAction
  @Override
  public String getName() {
    if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }
    if (myQualifiedName.isEmpty()) return null;
    int index = myQualifiedName.lastIndexOf('.');
    if (index < 0) {
      return myQualifiedName;
    }
    else {
      return myQualifiedName.substring(index + 1);
    }
  }

  @Override
  public void handleQualifiedNameChange(@Nonnull String newQualifiedName) {

  }

  @RequiredWriteAction
  @Override
  @Nullable
  public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
    checkSetName(name);
    PsiDirectory[] dirs = getDirectories();
    for (PsiDirectory dir : dirs) {
      dir.setName(name);
    }
    String nameAfterRename = PsiUtilCore.getQualifiedNameAfterRename(getQualifiedName(), name);
    return myPackageManager.findPackage(nameAfterRename, myExtensionClass);
  }

  public void checkSetName(@Nonnull String name) throws IncorrectOperationException {
    PsiDirectory[] dirs = getDirectories();
    for (PsiDirectory dir : dirs) {
      dir.checkSetName(name);
    }
  }

  @Override
  public PsiPackage getParentPackage() {
    if (myQualifiedName.isEmpty()) return null;
    int lastDot = myQualifiedName.lastIndexOf('.');
    if (lastDot < 0) {
      return myPackageManager.findPackage("", myExtensionClass);
    }
    else {
      return myPackageManager.findPackage(myQualifiedName.substring(0, lastDot), myExtensionClass);
    }
  }

  @Nonnull
  @Override
  public PsiPackage[] getSubPackages() {
    return getSubPackages(GlobalSearchScope.allScope(getProject()));
  }

  @Nonnull
  @Override
  public PsiPackage[] getSubPackages(@Nonnull GlobalSearchScope scope) {
    return getSubPackages(this, scope);
  }

  protected abstract ArrayFactory<? extends PsiPackage> getPackageArrayFactory();

  @Nonnull
  public PsiPackage[] getSubPackages(@Nonnull PsiPackage psiPackage, @Nonnull GlobalSearchScope scope) {
    final Map<String, PsiPackage> packagesMap = new HashMap<String, PsiPackage>();
    final String qualifiedName = psiPackage.getQualifiedName();
    for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
      PsiDirectory[] subDirs = dir.getSubdirectories();
      for (PsiDirectory subDir : subDirs) {
        final PsiPackage aPackage = myPackageManager.findPackage(subDir, myExtensionClass);
        if (aPackage != null) {
          final String subQualifiedName = aPackage.getQualifiedName();
          if (subQualifiedName.startsWith(qualifiedName) && !packagesMap.containsKey(subQualifiedName)) {
            packagesMap.put(aPackage.getQualifiedName(), aPackage);
          }
        }
      }
    }

    packagesMap.remove(qualifiedName);    // avoid SOE caused by returning a package as a subpackage of itself
    return ContainerUtil.toArray(packagesMap.values(), getPackageArrayFactory());
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    LOG.error("method not implemented");
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiElement getParent() {
    return getParentPackage();
  }

  @Override
  @Nullable
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  @Nullable
  public TextRange getTextRange() {
    return null;
  }

  @Override
  public int getStartOffsetInParent() {
    return -1;
  }

  @Override
  public int getTextLength() {
    return -1;
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return null;
  }

  @Override
  public int getTextOffset() {
    return -1;
  }

  @Override
  @Nullable
  public String getText() {
    return null;
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO throw new InsupportedOperationException()
  }

  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return false;
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return false;
  }

  @Override
  public PsiElement copy() {
    LOG.error("method not implemented");
    return null;
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void delete() throws IncorrectOperationException {
    checkDelete();
    PsiDirectory[] dirs = getDirectories();
    for (PsiDirectory dir : dirs) {
      dir.delete();
    }
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    for (PsiDirectory dir : getDirectories()) {
      dir.checkDelete();
    }
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public boolean isWritable() {
    PsiDirectory[] dirs = getDirectories();
    for (PsiDirectory dir : dirs) {
      if (!dir.isWritable()) return false;
    }
    return true;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitElement(this);
  }

  public String toString() {
    return "PsiPackageBase:" + getQualifiedName();
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public boolean isPhysical() {
    return true;
  }

  @Override
  public ASTNode getNode() {
    return null;
  }

  @Override
  public boolean isValid() {
    return !getAllDirectories(true).isEmpty();
  }

  @Override
  public boolean canNavigate() {
    return isValid();
  }

  @Override
  public ItemPresentation getPresentation() {
    return ItemPresentationProviders.getItemPresentation(this);
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Collection<PsiDirectory> allDirectories = getAllDirectories(true);
    if(!allDirectories.isEmpty()) {
      allDirectories.iterator().next().navigate(requestFocus);
    }
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    info.put("packageName", getName());
    info.put("packageQualifiedName", getQualifiedName());
  }
}
