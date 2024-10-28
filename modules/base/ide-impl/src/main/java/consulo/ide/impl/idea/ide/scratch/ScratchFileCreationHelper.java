/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.scratch.ScratchFileService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author gregsh
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ScratchFileCreationHelper implements LanguageExtension {
    private static final ExtensionPointCacheKey<ScratchFileCreationHelper, ByLanguageValue<ScratchFileCreationHelper>> KEY =
        ExtensionPointCacheKey.create("ScratchFileCreationHelper", LanguageOneToOne.build(new ScratchFileCreationHelper() {
            @Nonnull
            @Override
            public Language getLanguage() {
                return Language.ANY;
            }
        }));

    @Nonnull
    public static ScratchFileCreationHelper forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionPoint(ScratchFileCreationHelper.class).getOrBuildCache(KEY).requiredGet(language);
    }

    /**
     * Override to change the default initial text for a scratch file stored in {@link Context#text} field.
     * Return true if the text is set up as needed and no further considerations are necessary.
     */
    public boolean prepareText(@Nonnull Project project, @Nonnull Context context, @Nonnull DataContext dataContext) {
        return false;
    }

    public void beforeCreate(@Nonnull Project project, @Nonnull Context context) {
    }

    public static class Context {
        @Nonnull
        public String text = "";
        public Language language;
        public int caretOffset;

        public String filePrefix;
        public Supplier<Integer> fileCounter;
        public String fileExtension;

        public ScratchFileService.Option createOption = ScratchFileService.Option.create_new_always;
        public IdeView ideView;
    }

    @Nullable
    public static PsiFile parseHeader(@Nonnull Project project, @Nonnull Language language, @Nonnull String text) {
        LanguageFileType fileType = language.getAssociatedFileType();
        CharSequence fileSnippet = StringUtil.first(text, 10 * 1024, false);
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        return fileFactory.createFileFromText(
            PathUtil.makeFileName("a", fileType == null ? "" : fileType.getDefaultExtension()),
            language,
            fileSnippet
        );
    }

    @Nonnull
    @RequiredUIAccess
    public static String reformat(@Nonnull Project project, @Nonnull Language language, @Nonnull String text) {
        return CommandProcessor.getInstance().<String>newCommand()
            .project(project)
            .inWriteAction()
            .compute(() -> {
                PsiFile psi = parseHeader(project, language, text);
                if (psi != null) {
                    CodeStyleManager.getInstance(project).reformat(psi);
                }
                return psi == null ? text : psi.getText();
            });
    }
}
