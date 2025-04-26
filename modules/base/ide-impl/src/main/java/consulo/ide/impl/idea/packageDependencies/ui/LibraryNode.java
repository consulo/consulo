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

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.language.psi.PsiFile;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;

import java.util.Set;

public class LibraryNode extends PackageDependenciesNode {

    private final OrderEntry myLibraryOrJdk;

    public LibraryNode(OrderEntry libraryOrJdk, Project project) {
        super(project);
        myLibraryOrJdk = libraryOrJdk;
    }

    @Override
    public void fillFiles(Set<PsiFile> set, boolean recursively) {
        super.fillFiles(set, recursively);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            PackageDependenciesNode child = (PackageDependenciesNode) getChildAt(i);
            child.fillFiles(set, true);
        }
    }

    @Override
    public String toString() {
        return myLibraryOrJdk.getPresentableName();
    }

    @Override
    public int getWeight() {
        return 2;
    }

    @Override
    public boolean equals(Object o) {
        if (isEquals()) {
            return super.equals(o);
        }
        if (this == o) return true;
        if (!(o instanceof LibraryNode)) return false;

        final LibraryNode libraryNode = (LibraryNode) o;

        if (!myLibraryOrJdk.equals(libraryNode.myLibraryOrJdk)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myLibraryOrJdk.hashCode();
    }

    @Override
    public Image getIcon() {
        if (myLibraryOrJdk instanceof ModuleExtensionWithSdkOrderEntry entry) {
            Sdk sdk = entry.getSdk();
            if (sdk != null) {
                return SdkUtil.getIcon(sdk);
            }
            return PlatformIconGroup.actionsHelp();
        }
        else {
            return PlatformIconGroup.nodesPplibfolder();
        }
    }
}
