// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.documentation;

import com.uber.nullaway.annotations.Contract;
import consulo.ide.impl.idea.codeInsight.navigation.DocPreviewUtil;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiQualifiedNamedElement;
import org.jspecify.annotations.Nullable;

/**
 * @author gregsh
 */
public class QuickDocUtil {
    @Contract("_, _, _, null -> null")
    public static String inferLinkFromFullDocumentation(
        DocumentationProvider provider,
        PsiElement element,
        PsiElement originalElement,
        @Nullable String navigationInfo
    ) {
        if (navigationInfo != null) {
            String fqn =
                element instanceof PsiQualifiedNamedElement qualifiedNamedElement ? qualifiedNamedElement.getQualifiedName() : null;
            String fullText = provider.generateDoc(element, originalElement);
            return HintUtil.prepareHintText(DocPreviewUtil.buildPreview(navigationInfo, fqn, fullText), HintUtil.getInformationHint());
        }
        return null;
    }
}
