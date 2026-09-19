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

package consulo.language.editor.hierarchy;

import consulo.application.dumb.IndexNotReadyException;
import consulo.colorScheme.TextAttributes;
import consulo.component.util.Iconable;
import consulo.ui.ex.util.CompositeAppearance;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

public abstract class HierarchyNodeDescriptor extends NodeDescriptor {
    private static final TextAttributesKey INVALID_PREFIX = TextAttributesKey.of("$INVALID_PREFIX");
    private static final TextAttributesKey NUMBER_OF_USAGES = TextAttributesKey.of("$NUMBER_OF_USAGES");

    private final SmartPsiElementPointer mySmartPointer;
    private final Project myProject;

    protected CompositeAppearance myHighlightedText;
    private Object[] myCachedChildren = null;
    protected final boolean myIsBase;

    protected HierarchyNodeDescriptor(
        Project project,
        NodeDescriptor parentDescriptor,
        PsiElement element,
        boolean isBase
    ) {
        super(parentDescriptor);
        myProject = project;
        mySmartPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element);
        myHighlightedText = new CompositeAppearance();
        myName = "";
        myIsBase = isBase;
    }

    public final @Nullable PsiElement getPsiElement() {
        return mySmartPointer.getElement();
    }

    /**
     * The element this node stands for as far as the hierarchy is concerned. A call hierarchy node holds a
     * call site but belongs to the method enclosing it, so the two notions part company there.
     */
    public @Nullable PsiElement getHierarchyElement() {
        return getPsiElement();
    }

    /**
     * The element opening this node navigates to - the call site itself, where {@link #getHierarchyElement()}
     * answers the method around it.
     */
    public @Nullable PsiElement getOpenFileElement() {
        return getHierarchyElement();
    }

    /**
     * Whether this node offers its element to the platform delete action.
     */
    public boolean canBeDeleted() {
        return false;
    }

    /**
     * Name of this node's element for the delete action's local-history entry.
     */
    public @Nullable String getQualifiedName() {
        return null;
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    @Override
    public boolean update() {
        PsiElement element = mySmartPointer.getElement();
        if (element == null) {
            return true;
        }

        int flags = Iconable.ICON_FLAG_VISIBILITY;
        if (isMarkReadOnly()) {
            flags |= Iconable.ICON_FLAG_READ_STATUS;
        }

        Image icon = null;
        try {
            icon = IconDescriptorUpdaters.getIcon(element, flags);
        }
        catch (IndexNotReadyException ignored) {
        }

        ColorValue color = null;
        if (isMarkModified()) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
            if (virtualFile != null) {
                color = FileStatusManager.getInstance(myProject).getStatus(virtualFile).getColor();
            }
        }
        if (CopyPasteManager.getInstance().isCutElement(element)) {
            color = CopyPasteManager.CUT_COLOR;
        }

        boolean changes = !Comparing.equal(icon, getIcon()) || !Comparing.equal(color, myColor);
        setIcon(icon);
        myColor = color;
        return changes;
    }

    @Override
    public final Object getElement() {
        return this;
    }

    public @Nullable PsiFile getContainingFile() {
        PsiElement element = getPsiElement();
        return element != null ? element.getContainingFile() : null;
    }

    public boolean isValid() {
        return getPsiElement() != null;
    }

    public final Object[] getCachedChildren() {
        return myCachedChildren;
    }

    public final void setCachedChildren(Object[] cachedChildren) {
        myCachedChildren = cachedChildren;
    }

    protected final boolean isMarkReadOnly() {
        return true;
    }

    protected final boolean isMarkModified() {
        return true;
    }

    public final CompositeAppearance getHighlightedText() {
        return myHighlightedText;
    }

    protected static TextAttributes getInvalidPrefixAttributes() {
        return attributesOf(INVALID_PREFIX);
    }

    protected static TextAttributes getUsageCountPrefixAttributes() {
        return attributesOf(NUMBER_OF_USAGES);
    }

    private static TextAttributes attributesOf(TextAttributesKey key) {
        return EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getAttributes(key);
    }

    protected static TextAttributes getPackageNameAttributes() {
        return getUsageCountPrefixAttributes();
    }

    @Override
    public boolean expandOnDoubleClick() {
        return false;
    }
}
