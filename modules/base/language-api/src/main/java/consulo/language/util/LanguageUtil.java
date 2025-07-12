/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.InjectableLanguage;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.file.LanguageFileType;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.LanguageSubstitutor;
import consulo.language.psi.LanguageSubstitutors;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateLanguage;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public final class LanguageUtil {
    public static final Comparator<Language> LANGUAGE_COMPARATOR = Comparator.comparing(Language::getDisplayName);

    private LanguageUtil() {
    }

    @Nonnull
    public static JBIterable<Language> getBaseLanguages(@Nonnull Language language) {
        return JBIterable.generate(language, Language::getBaseLanguage);
    }

    @Nullable
    public static Language getLanguageForPsi(@Nonnull Project project, @Nullable VirtualFile file) {
        return getLanguageForPsi(project, file, null);
    }

    @Nullable
    public static Language getLanguageForPsi(@Nonnull Project project, @Nullable VirtualFile file, @Nullable FileType fileType) {
        if (file == null) {
            return null;
        }
        // a copy-paste of getFileLanguage(file)
        Language explicit = file instanceof LightVirtualFile ? ((LightVirtualFile)file).getLanguage() : null;
        Language fileLanguage = explicit != null ? explicit : getFileTypeLanguage(fileType != null ? fileType : file.getFileType());

        if (fileLanguage == null) {
            return null;
        }
        // run generic file-level substitutors, e.g. for scratches
        for (LanguageSubstitutor substitutor : LanguageSubstitutor.forLanguage(Language.ANY)) {
            Language language = substitutor.getLanguage(file, project);
            if (language != null && language != Language.ANY) {
                fileLanguage = language;
                break;
            }
        }
        return LanguageSubstitutors.substituteLanguage(fileLanguage, file, project);
    }

    @Nullable
    public static Language getFileLanguage(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        Language l = file instanceof LightVirtualFile ? ((LightVirtualFile)file).getLanguage() : null;
        return l != null ? l : getFileTypeLanguage(file.getFileType());
    }

    @Nullable
    public static Language getFileTypeLanguage(@Nullable FileType fileType) {
        return fileType instanceof LanguageFileType languageFileType ? languageFileType.getLanguage() : null;
    }

    @Nullable
    public static FileType getLanguageFileType(@Nullable Language language) {
        return language == null ? null : language.getAssociatedFileType();
    }

    @Nonnull
    @RequiredReadAction
    public static ParserDefinition.SpaceRequirements canStickTokensTogetherByLexer(ASTNode left, ASTNode right, Lexer lexer) {
        String textStr = left.getText() + right.getText();

        lexer.start(textStr, 0, textStr.length());
        if (lexer.getTokenType() != left.getElementType()) {
            return ParserDefinition.SpaceRequirements.MUST;
        }
        if (lexer.getTokenEnd() != left.getTextLength()) {
            return ParserDefinition.SpaceRequirements.MUST;
        }
        lexer.advance();
        if (lexer.getTokenEnd() != textStr.length()) {
            return ParserDefinition.SpaceRequirements.MUST;
        }
        if (lexer.getTokenType() != right.getElementType()) {
            return ParserDefinition.SpaceRequirements.MUST;
        }
        return ParserDefinition.SpaceRequirements.MAY;
    }

    @Nonnull
    public static Language[] getLanguageDialects(@Nonnull Language base) {
        List<Language> list = ContainerUtil.findAll(
            Language.getRegisteredLanguages(),
            language -> language.getBaseLanguage() == base
        );
        return list.toArray(new Language[list.size()]);
    }

    public static boolean isInTemplateLanguageFile(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }

        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            return false;
        }

        Language language = psiFile.getViewProvider().getBaseLanguage();
        return language instanceof TemplateLanguage;
    }

    public static boolean isInjectableLanguage(@Nonnull Language language) {
        if (language == Language.ANY) {
            return false;
        }
        if (language.getID().startsWith("$")) {
            return false;
        }
        if (language instanceof InjectableLanguage) {
            return true;
        }
        if (language instanceof TemplateLanguage) {
            return false;
        }
        return ParserDefinition.forLanguage(language) != null;
    }

    // FIXME [VISTALL] we really need this?
    public static boolean isFileLanguage(@Nonnull Language language) {
        if (language instanceof InjectableLanguage) {
            return false;
        }
        if (ParserDefinition.forLanguage(language) == null) {
            return false;
        }
        LanguageFileType type = language.getAssociatedFileType();
        if (type == null || StringUtil.isEmpty(type.getDefaultExtension())) {
            return false;
        }
        String name = language.getDisplayName().get();
        if (StringUtil.isEmpty(name) || name.startsWith("<") || name.startsWith("[")) {
            return false;
        }
        return StringUtil.isNotEmpty(type.getDefaultExtension());
    }

    @Nonnull
    public static List<Language> getFileLanguages() {
        List<Language> result = new ArrayList<>();
        for (Language language : Language.getRegisteredLanguages()) {
            if (!isFileLanguage(language)) {
                continue;
            }
            result.add(language);
        }
        Collections.sort(result, LANGUAGE_COMPARATOR);
        return result;
    }

    @Nonnull
    @RequiredReadAction
    public static Language getRootLanguage(@Nonnull PsiElement element) {
        FileViewProvider provider = element.getContainingFile().getViewProvider();
        Set<Language> languages = provider.getLanguages();
        if (languages.size() > 1) {
            PsiElement current = element;
            while (current != null) {
                Language language = current.getLanguage();
                if (languages.contains(language)) {
                    return language;
                }
                current = current.getParent();
            }
        }
        return provider.getBaseLanguage();
    }

    @Nonnull
    public static JBIterable<Language> hierarchy(@Nonnull Language language) {
        return JBIterable.generate(language, Language::getBaseLanguage);
    }
}
