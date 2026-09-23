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
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPackage;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PackageNode extends PackageDependenciesNode {
    private String myPackageName;
    private final String myPackageQName;
    private final PsiPackage myPackage;

    @RequiredReadAction
    public PackageNode(PsiPackage aPackage, boolean showFQName) {
        super(aPackage.getProject());
        myPackage = aPackage;
        myPackageName = showFQName ? aPackage.getQualifiedName() : aPackage.getName();
        if (StringUtil.isEmpty(myPackageName)) {
            myPackageName = AnalysisScopeLocalize.dependenciesTreeNodeDefaultPackageAbbreviation().get();
        }
        myPackageQName = StringUtil.nullize(aPackage.getQualifiedName());
    }

    @Override
    public void fillFiles(Set<PsiFile> set, boolean recursively) {
        super.fillFiles(set, recursively);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            PackageDependenciesNode child = (PackageDependenciesNode) getChildAt(i);
            if (child instanceof FileNode || recursively) {
                child.fillFiles(set, true);
            }
        }
    }

    @Override
    public String toString() {
        return myPackageName;
    }

    public void setPackageName(String packageName) {
        myPackageName = packageName;
    }

    public String getPackageQName() {
        return myPackageQName;
    }

    @Override
    public PsiElement getPsiElement() {
        return myPackage;
    }

    @Override
    public int getWeight() {
        return 3;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (isEquals()) {
            return super.equals(o);
        }
        if (this == o) {
            return true;
        }
        return o instanceof PackageNode that
            && myPackageName.equals(that.myPackageName)
            && Objects.equals(myPackageQName, that.myPackageQName);
    }

    @Override
    public int hashCode() {
        return 29 * myPackageName.hashCode() + Objects.hashCode(myPackageQName);
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesParameter();
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return myPackage != null && myPackage.isValid();
    }

    @Override
    public boolean canSelectInLeftTree(Map<PsiFile, Set<PsiFile>> deps) {
        Set<PsiFile> files = deps.keySet();
        String packageName = myPackageQName;
        for (PsiFile file : files) {
            String qualifiedName = QualifiedNameProviderUtil.getQualifiedName(file);
            if (Comparing.equal(packageName, qualifiedName)) {
                return true;
            }
        }
        return false;
    }
}
