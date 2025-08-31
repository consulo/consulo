// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection.reference;

import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiNamedElement;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.List;

/**
 * Manager of the reference graph for a global inspection run.
 *
 * @author anna
 * @see GlobalInspectionContext#getRefManager()
 */
public interface RefManager {
    /**
     * Runs the specified visitor through all elements in the reference graph.
     *
     * @param visitor the visitor to run.
     */
    void iterate(@Nonnull RefVisitor visitor);

    /**
     * Returns the analysis scope for which the reference graph has been built.
     *
     * @return the analysis scope.
     */
    @Nullable
    AnalysisScope getScope();

    /**
     * Returns the project for which the reference graph has been built.
     *
     * @return the project instance.
     */
    @Nonnull
    Project getProject();

    /**
     * Returns the reference graph node pointing to the project for which the reference
     * graph has been built.
     *
     * @return the node for the project.
     */
    @Nonnull
    RefProject getRefProject();

    /**
     * Creates (if necessary) and returns the reference graph node for the specified module.
     *
     * @param module the module for which the reference graph node is requested.
     * @return the node for the module, or null if {@code module} is null.
     */
    @Nullable
    RefModule getRefModule(@Nullable Module module);

    /**
     * Creates (if necessary) and returns the reference graph node for the specified PSI element.
     *
     * @param elem the element for which the reference graph node is requested.
     * @return the node for the element, or null if the element is not valid or does not have
     * a corresponding reference graph node type (is not a field, method, class or file).
     */
    @Nullable
    RefElement getReference(@Nullable PsiElement elem);

    /**
     * Creates (if necessary) and returns the reference graph node for the PSI element specified by its type and FQName.
     *
     * @param type   {@link SmartRefElementPointer#FILE, etc.}
     * @param fqName fully qualified name for the element
     * @return the node for the element, or null if the element is not found or does not have
     * a corresponding reference graph node type.
     */
    @Nullable
    RefEntity getReference(String type, String fqName);

    long getLastUsedMask();

    <T> T getExtension(@Nonnull Key<T> key);

    @Nullable
    String getType(@Nonnull RefEntity ref);

    @Nonnull
    RefEntity getRefinedElement(@Nonnull RefEntity ref);

    @Nullable
    default Element export(@Nonnull RefEntity entity, @Nonnull Element parent, int actualLine) {
        Element element = export(entity, actualLine);
        if (element == null) {
            return null;
        }
        parent.addContent(element);
        return element;
    }

    @Nullable
    default Element export(@Nonnull RefEntity entity, int actualLine) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    String getGroupName(@Nonnull RefElement entity);

    boolean belongsToScope(@Nullable PsiElement psiElement);

    @Nullable
    String getQualifiedName(@Nullable RefEntity refEntity);

    void removeRefElement(@Nonnull RefElement refElement, @Nonnull List<RefElement> deletedRefs);

    @Nonnull
    PsiManager getPsiManager();

    /**
     * @return false if no {@link consulo.ide.impl.idea.codeInspection.lang.RefManagerExtension} was registered for language and is not covered by default implementation for PsiClassOwner
     * true, otherwise
     */
    default boolean isInGraph(VirtualFile file) {
        return true;
    }

    @Nullable
    default PsiNamedElement getContainerElement(@Nonnull PsiElement element) {
        return null;
    }
}
