// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.util;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.parser.ParserDefinition;
import consulo.language.version.LanguageVersion;
import consulo.virtualFileSystem.VirtualFile;

public final class DefaultCrcCalculator extends AbstractCrcCalculator {
    public static final DefaultCrcCalculator INSTANCE = new DefaultCrcCalculator();

    private DefaultCrcCalculator() {
    }

    @Override
    public boolean isApplicable(ProjectSystemId systemId, VirtualFile file) {
        return true;
    }

    @Override
    public boolean isIgnoredToken(
        IElementType tokenType,
        CharSequence tokenText,
        ParserDefinition parserDefinition,
        LanguageVersion languageVersion
    ) {
        TokenSet ignoredTokens = TokenSet.orSet(
            parserDefinition.getCommentTokens(languageVersion),
            parserDefinition.getWhitespaceTokens(languageVersion)
        );
        return ignoredTokens.contains(tokenType);
    }
}
