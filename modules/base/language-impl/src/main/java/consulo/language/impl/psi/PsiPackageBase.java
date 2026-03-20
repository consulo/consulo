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
package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.Queryable;
import consulo.application.util.query.Query;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.DebugUtil;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.module.content.DirectoryIndex;
import consulo.module.extension.ModuleExtension;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationProvider;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;

public abstract class PsiPackageBase extends PsiElementBase implements PsiPackage, Queryable {
    private static final Logger LOG = Logger.getInstance(PsiPackageBase.class);

    protected final PsiManager myManager;
    protected final PsiPackageManager myPackageManager;
    private final Class<? extends ModuleExtension> myExtensionClass;
    private final String myQualifiedName;

    public PsiPackageBase(
        PsiManager manager,
        PsiPackageManager packageManager,
        Class<? extends ModuleExtension> extensionClass,
        String qualifiedName
    ) {
        myManager = manager;
        myPackageManager = packageManager;
        myExtensionClass = extensionClass;
        myQualifiedName = qualifiedName;
    }

    @RequiredReadAction
    protected Collection<PsiDirectory> getAllDirectories(boolean inLibrarySources) {
        List<PsiDirectory> directories = new ArrayList<>();
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
            myManager == ((PsiPackageBase) o).myManager &&
            myQualifiedName.equals(((PsiPackageBase) o).myQualifiedName);
    }

    @Override
    public int hashCode() {
        return myQualifiedName.hashCode();
    }

    
    @Override
    public String getQualifiedName() {
        return myQualifiedName;
    }

    
    @Override
    @RequiredReadAction
    public PsiDirectory[] getDirectories() {
        Collection<PsiDirectory> collection = getAllDirectories(false);
        return ContainerUtil.toArray(collection, PsiDirectory.ARRAY_FACTORY);
    }

    
    @Override
    @RequiredReadAction
    public PsiDirectory[] getDirectories(GlobalSearchScope scope) {
        List<PsiDirectory> result = null;
        boolean includeLibrarySources = scope.isForceSearchingInLibrarySources();
        Collection<PsiDirectory> directories = getAllDirectories(includeLibrarySources);
        for (PsiDirectory directory : directories) {
            if (scope.contains(directory.getVirtualFile())) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(directory);
            }
        }
        return result == null ? PsiDirectory.EMPTY_ARRAY : result.toArray(new PsiDirectory[result.size()]);
    }

    @Override
    @RequiredReadAction
    public @Nullable String getName() {
        if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
            getApplication().assertReadAccessAllowed();
        }
        if (myQualifiedName.isEmpty()) {
            return null;
        }
        int index = myQualifiedName.lastIndexOf('.');
        if (index < 0) {
            return myQualifiedName;
        }
        else {
            return myQualifiedName.substring(index + 1);
        }
    }

    @Override
    public void handleQualifiedNameChange(String newQualifiedName) {
    }

    @RequiredWriteAction
    @Override
    public @Nullable PsiElement setName(String name) throws IncorrectOperationException {
        checkSetName(name);
        PsiDirectory[] dirs = getDirectories();
        for (PsiDirectory dir : dirs) {
            dir.setName(name);
        }
        String nameAfterRename = PsiUtilCore.getQualifiedNameAfterRename(getQualifiedName(), name);
        return myPackageManager.findPackage(nameAfterRename, myExtensionClass);
    }

    @Override
    @RequiredReadAction
    public void checkSetName(String name) throws IncorrectOperationException {
        for (PsiDirectory dir : getDirectories()) {
            dir.checkSetName(name);
        }
    }

    @Override
    @RequiredReadAction
    public PsiPackage getParentPackage() {
        if (myQualifiedName.isEmpty()) {
            return null;
        }
        int lastDot = myQualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return myPackageManager.findPackage("", myExtensionClass);
        }
        else {
            return myPackageManager.findPackage(myQualifiedName.substring(0, lastDot), myExtensionClass);
        }
    }

    
    @Override
    @RequiredReadAction
    public PsiPackage[] getSubPackages() {
        return getSubPackages(GlobalSearchScope.allScope(getProject()));
    }

    
    @Override
    @RequiredReadAction
    public PsiPackage[] getSubPackages(GlobalSearchScope scope) {
        return getSubPackages(this, scope);
    }

    protected abstract IntFunction<? extends PsiPackage[]> getPackageArrayFactory();

    
    @RequiredReadAction
    public PsiPackage[] getSubPackages(PsiPackage psiPackage, GlobalSearchScope scope) {
        Map<String, PsiPackage> packagesMap = new HashMap<>();
        String qualifiedName = psiPackage.getQualifiedName();
        for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
            PsiDirectory[] subDirs = dir.getSubdirectories();
            for (PsiDirectory subDir : subDirs) {
                PsiPackage aPackage = myPackageManager.findPackage(subDir, myExtensionClass);
                if (aPackage != null) {
                    String subQualifiedName = aPackage.getQualifiedName();
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
    @RequiredReadAction
    public PsiManager getManager() {
        return myManager;
    }

    
    @Override
    @RequiredReadAction
    public PsiElement[] getChildren() {
        LOG.error("method not implemented");
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getParent() {
        return getParentPackage();
    }

    @Override
    public @Nullable PsiFile getContainingFile() {
        return null;
    }

    
    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return TextRange.EMPTY_RANGE;
    }

    @Override
    @RequiredReadAction
    public int getStartOffsetInParent() {
        return -1;
    }

    @Override
    @RequiredReadAction
    public int getTextLength() {
        return -1;
    }

    @Override
    @RequiredReadAction
    public PsiElement findElementAt(int offset) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return -1;
    }

    @Override
    @RequiredReadAction
    public @Nullable String getText() {
        return null;
    }

    
    @Override
    @RequiredReadAction
    public char[] textToCharArray() {
        return ArrayUtil.EMPTY_CHAR_ARRAY; // TODO throw new UnsupportedOperationException()
    }

    @Override
    @RequiredReadAction
    public boolean textMatches(CharSequence text) {
        return false;
    }

    @Override
    @RequiredReadAction
    public boolean textMatches(PsiElement element) {
        return false;
    }

    @Override
    public PsiElement copy() {
        LOG.error("method not implemented");
        return null;
    }

    @Override
    public PsiElement add(PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    @RequiredWriteAction
    public PsiElement addBefore(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    @RequiredWriteAction
    public PsiElement addAfter(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public void checkAdd(PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    @RequiredWriteAction
    public void delete() throws IncorrectOperationException {
        checkDelete();
        PsiDirectory[] dirs = getDirectories();
        for (PsiDirectory dir : dirs) {
            dir.delete();
        }
    }

    @Override
    @RequiredWriteAction
    public void checkDelete() throws IncorrectOperationException {
        for (PsiDirectory dir : getDirectories()) {
            dir.checkDelete();
        }
    }

    @Override
    @RequiredWriteAction
    public PsiElement replace(PsiElement newElement) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    @RequiredReadAction
    public boolean isWritable() {
        PsiDirectory[] dirs = getDirectories();
        for (PsiDirectory dir : dirs) {
            if (!dir.isWritable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void accept(PsiElementVisitor visitor) {
        visitor.visitElement(this);
    }

    @Override
    public String toString() {
        return "PsiPackageBase:" + getQualifiedName();
    }

    @Override
    @RequiredReadAction
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
    @RequiredReadAction
    public boolean isValid() {
        return !getAllDirectories(true).isEmpty();
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return isValid();
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProvider.getItemPresentation(this);
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        Collection<PsiDirectory> allDirectories = getAllDirectories(true);
        if (!allDirectories.isEmpty()) {
            allDirectories.iterator().next().navigate(requestFocus);
        }
    }

    @Override
    @RequiredReadAction
    public void putInfo(Map<String, String> info) {
        info.put("packageName", getName());
        info.put("packageQualifiedName", getQualifiedName());
    }
}
