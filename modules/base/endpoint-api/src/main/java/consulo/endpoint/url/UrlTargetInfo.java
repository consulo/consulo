// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.language.psi.PsiElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface UrlTargetInfo {
    List<String> getSchemes();

    List<Authority> getAuthorities();

    UrlPath getPath();

    default Image getIcon() {
        return PlatformIconGroup.nodesPpweb();
    }

    default boolean isDeprecated() {
        return false;
    }

    /**
     * A set of upper-cased HTTP method names, supported by this target. Empty Set means that any method is supported.
     */
    default Set<String> getMethods() {
        return Set.of();
    }

    /**
     * Location of Url target declaration. Represented by a class name (e.g MyController), or a file name.
     */
    default String getSource() {
        return "";
    }

    default @Nullable PsiElement getDocumentationPsiElement() {
        return resolveToPsiElement();
    }

    @Nullable PsiElement resolveToPsiElement();

    default Iterable<UrlQueryParameter> getQueryParameters() {
        return List.of();
    }

    default Set<String> getContentTypes() {
        return Set.of();
    }
}
