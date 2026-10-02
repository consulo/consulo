// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.util.CommonFakeNavigatablePomTarget;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.IncorrectOperationException;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static consulo.endpoint.url.UrlConversionConstants.STANDARD_PATH_VARIABLE_NAME_PATTERN;
import static java.util.Objects.requireNonNull;

/**
 * Represents a declaration of a PathVariable - a specially marked part of the URL in the endpoint declaration:
 * EXAMPLE: in {@code "/users/{id}/delete"} it is {@code "{id}"}
 *
 * @see PathVariableDeclaringReference
 */
public class PathVariablePsiElement extends CommonFakeNavigatablePomTarget {
    private final Pattern myNameValidationPattern;
    private final PathVariablePomTarget myVariablePomTarget;
    private final boolean myForceFindUsages;

    private PathVariablePsiElement(PathVariablePomTarget pomTarget, Pattern nameValidationPattern, boolean forceFindUsages) {
        super(pomTarget.getScope().getProject(), pomTarget);
        myVariablePomTarget = pomTarget;
        myNameValidationPattern = nameValidationPattern;
        myForceFindUsages = forceFindUsages;
    }

    private PathVariablePsiElement(String name,
                                   PsiElement scope,
                                   TextRange textRange,
                                   Pattern nameValidationPattern,
                                   SemDefinitionProvider semDefinitionProvider) {
        this(new PathVariablePomTarget(name, scope, textRange, semDefinitionProvider), nameValidationPattern, true);
    }

    public static PathVariablePsiElement create(String varName,
                                                PsiElement scope,
                                                TextRange textRange,
                                                SemDefinitionProvider semDefinitionProvider) {
        return new PathVariablePsiElement(varName, scope, textRange, STANDARD_PATH_VARIABLE_NAME_PATTERN, semDefinitionProvider);
    }

    public static PathVariablePsiElement create(String varName, PsiElement scope, SemDefinitionProvider semDefinitionProvider) {
        return create(varName, scope, ElementManipulators.getValueTextRange(scope), semDefinitionProvider);
    }

    public static PathVariablePsiElement create(String varName,
                                                PsiElement scope,
                                                TextRange textRange,
                                                Pattern nameValidationPattern,
                                                SemDefinitionProvider semDefinitionProvider) {
        return new PathVariablePsiElement(varName, scope, textRange, nameValidationPattern, semDefinitionProvider);
    }

    public static @Nullable PathVariablePsiElement merge(List<PathVariablePsiElement> elements) {
        if (elements.isEmpty()) {
            return null;
        }
        if (elements.size() == 1) {
            return elements.get(0);
        }
        return new MultiplePomTarget(elements.stream().map(e -> e.getVariablePomTarget()).collect(Collectors.toSet()));
    }

    public PathVariablePomTarget getVariablePomTarget() {
        return myVariablePomTarget;
    }

    @Override
    @RequiredReadAction
    public String getName() {
        return myVariablePomTarget.getName();
    }

    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return myVariablePomTarget.getTextRange();
    }

    public PathVariablePsiElement navigatingToDeclaration() {
        return new PathVariablePsiElement(myVariablePomTarget, myNameValidationPattern, false);
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        if (myForceFindUsages) {
            super.navigate(requestFocus);
            return;
        }
        VirtualFile file = PsiUtilCore.getVirtualFile(myVariablePomTarget.getScope());
        if (file == null) {
            return;
        }
        OpenFileDescriptorFactory.getInstance(getProject())
            .newBuilder(file)
            .offset(getTextOffset())
            .build()
            .navigate(requestFocus);
    }

    @Override
    public @Nullable PsiElement getParent() {
        return myVariablePomTarget.getScope();
    }

    public Pattern getNameValidationPattern() {
        return myNameValidationPattern;
    }

    @Override
    public PsiElement setName(String name) throws IncorrectOperationException {
        myVariablePomTarget.setName(name);
        return this;
    }

    @Override
    @RequiredReadAction
    public int getTextOffset() {
        return requireNonNull(getParent()).getTextOffset() + myVariablePomTarget.getTextRange().getStartOffset();
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return true;
    }

    @Override
    @RequiredReadAction
    public int getTextLength() {
        return getName().length();
    }

    @Override
    public String getTypeName() {
        return EndpointLocalize.microservicesUrlPathVariableTypeName().get();
    }

    @Override
    @RequiredReadAction
    public Image getIcon() {
        return PlatformIconGroup.nodesVariable();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathVariablePsiElement element)) {
            return false;
        }

        if (!myVariablePomTarget.equals(element.myVariablePomTarget)) {
            return false;
        }
        if (!getName().equals(element.getName())) {
            return false;
        }
        if (!getTextRange().equals(element.getTextRange())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = myVariablePomTarget.hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getTextRange().hashCode();
        return result;
    }

    private static class MultiplePomTarget extends PathVariablePsiElement {
        private final Set<PathVariablePomTarget> myPomTargets;

        private MultiplePomTarget(Set<PathVariablePomTarget> pomTargets) {
            super(
                requireNonNull(ContainerUtil.getFirstItem(pomTargets)).getName(),
                requireNonNull(ContainerUtil.getFirstItem(pomTargets)).getScope(),
                requireNonNull(ContainerUtil.getFirstItem(pomTargets)).getTextRange(),
                STANDARD_PATH_VARIABLE_NAME_PATTERN,
                requireNonNull(ContainerUtil.getFirstItem(pomTargets)).getSemDefinitionProvider()
            );
            myPomTargets = pomTargets;
        }

        @Override
        public boolean isEquivalentTo(@Nullable PsiElement another) {
            if (this == another) {
                return true;
            }
            if (!(another instanceof PathVariablePsiElement)) {
                return false;
            }
            if (another instanceof MultiplePomTarget multiplePomTarget) {
                return myPomTargets.equals(multiplePomTarget.myPomTargets);
            }
            PathVariablePomTarget variablePomTarget = ((PathVariablePsiElement) another).getVariablePomTarget();
            return ContainerUtil.exists(myPomTargets, pomTarget -> pomTarget.equals(variablePomTarget));
        }
    }
}
