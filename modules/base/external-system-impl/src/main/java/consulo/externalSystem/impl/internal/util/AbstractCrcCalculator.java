// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.util;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.externalSystem.util.ExternalSystemCrcCalculator;
import consulo.language.ast.IElementType;
import consulo.language.file.LanguageFileType;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.zip.CRC32;

public abstract class AbstractCrcCalculator implements ExternalSystemCrcCalculator {
    public abstract boolean isIgnoredToken(
        IElementType tokenType,
        CharSequence tokenText,
        ParserDefinition parserDefinition,
        LanguageVersion languageVersion
    );

    public Lexer createLexer(Project project, ParserDefinition parserDefinition, LanguageVersion languageVersion) {
        return parserDefinition.createLexer(languageVersion);
    }

    @Override
    public @Nullable Long calculateCrc(Project project, VirtualFile file, CharSequence fileText) {
        ParserDefinition parserDefinition = getParserDefinition(project.getApplication(), file.getFileType());
        if (parserDefinition == null) {
            return null;
        }
        return calculateCrc(project, fileText, parserDefinition);
    }

    private long calculateCrc(Project project, CharSequence charSequence, ParserDefinition parserDefinition) {
        LanguageVersion languageVersion = parserDefinition.getLanguage().getVersions()[0];
        Lexer lexer = createLexer(project, parserDefinition, languageVersion);
        CRC32 crc32 = new CRC32();
        lexer.start(charSequence);
        ProgressManager.checkCanceled();
        while (true) {
            IElementType tokenType = lexer.getTokenType();
            if (tokenType == null) {
                break;
            }
            CharSequence tokenText = charSequence.subSequence(lexer.getTokenStart(), lexer.getTokenEnd());
            update(crc32, tokenType, tokenText, parserDefinition, languageVersion);
            lexer.advance();
            ProgressManager.checkCanceled();
        }
        return crc32.getValue();
    }

    private void update(
        CRC32 crc32,
        IElementType tokenType,
        CharSequence tokenText,
        ParserDefinition parserDefinition,
        LanguageVersion languageVersion
    ) {
        if (isIgnoredToken(tokenType, tokenText, parserDefinition, languageVersion)) {
            return;
        }
        if (isBlank(tokenText)) {
            return;
        }
        update(crc32, tokenText);
    }

    private static void update(CRC32 crc32, CharSequence charSequence) {
        crc32.update(charSequence.length());
        for (int i = 0; i < charSequence.length(); i++) {
            crc32.update(charSequence.charAt(i));
        }
    }

    private static boolean isBlank(CharSequence charSequence) {
        for (int i = 0; i < charSequence.length(); i++) {
            char ch = charSequence.charAt(i);
            if (!Character.isWhitespace(ch) && !Character.isSpaceChar(ch)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable ParserDefinition getParserDefinition(Application application, FileType fileType) {
        if (fileType instanceof LanguageFileType languageFileType) {
            return ParserDefinition.forLanguage(application, languageFileType.getLanguage());
        }
        return null;
    }
}
