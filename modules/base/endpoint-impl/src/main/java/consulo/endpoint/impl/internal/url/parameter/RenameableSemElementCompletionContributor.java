// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.Plow;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.language.util.ProcessingContext;

@ExtensionImpl
public class RenameableSemElementCompletionContributor extends CompletionContributor {
    public RenameableSemElementCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider() {
            @Override
            @RequiredReadAction
            public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
                PsiElement position = parameters.getPosition();
                if (UastLanguagePlugin.byLanguage(position.getLanguage()) == null) {
                    return;
                }
                Plow<LookupElement> nameVariants = RenameableSemElementCompletionContributorUtil.getParameterNameVariants(position);
                if (nameVariants != null) {
                    nameVariants.processWith(e -> {
                        result.addElement(e);
                        return !result.isStopped();
                    });
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
