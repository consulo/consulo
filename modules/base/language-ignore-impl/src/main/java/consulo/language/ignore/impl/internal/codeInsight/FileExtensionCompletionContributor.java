// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.ignore.impl.internal.codeInsight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

@ExtensionImpl
public class FileExtensionCompletionContributor extends CompletionContributor {
    static final String EXTENSION_MASK = "*.";

    public FileExtensionCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), (parameters, context, result) -> {
            PsiElement current = parameters.getPosition();
            if (fileExtensionCompletionSupported(current.getText())) {
                for (FileType fileType : FileTypeRegistry.getInstance().getRegisteredFileTypes()) {
                    result.addElement(LookupElementBuilder.create(fileType.getDefaultExtension()).withIcon(fileType.getIcon()));
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return IgnoreLanguage.INSTANCE;
    }

    public static boolean fileExtensionCompletionSupported(String text) {
        return text.startsWith(EXTENSION_MASK) || text.contains("/" + EXTENSION_MASK);
    }
}
