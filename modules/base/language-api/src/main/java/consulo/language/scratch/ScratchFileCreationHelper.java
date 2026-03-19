/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.scratch;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.file.LanguageFileType;
import consulo.language.internal.LanguageInternal;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.PathUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author gregsh
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ScratchFileCreationHelper implements LanguageExtension {
    private static final ExtensionPointCacheKey<ScratchFileCreationHelper, ByLanguageValue<ScratchFileCreationHelper>> KEY =
        ExtensionPointCacheKey.create("ScratchFileCreationHelper", LanguageOneToOne.build(new ScratchFileCreationHelper() {
            
            @Override
            public Language getLanguage() {
                return Language.ANY;
            }
        }));

    
    public static ScratchFileCreationHelper forLanguage(Language language) {
        return Application.get().getExtensionPoint(ScratchFileCreationHelper.class).getOrBuildCache(KEY).requiredGet(language);
    }

    /**
     * Override to change the default initial text for a scratch file stored in {@link Context#text} field.
     * Return true if the text is set up as needed and no further considerations are necessary.
     */
    public boolean prepareText(Project project, Context context, DataContext dataContext) {
        return false;
    }

    public void beforeCreate(Project project, Context context) {
    }

    public static class Context {
        
        public String text = "";
        public Language language;
        public int caretOffset;

        public String filePrefix;
        public Supplier<Integer> fileCounter;
        public String fileExtension;

        public ScratchFileService.Option createOption = ScratchFileService.Option.create_new_always;

        public DataContext dataProvider;
    }

    public static @Nullable PsiFile parseHeader(Project project, Language language, String text) {
        LanguageFileType fileType = language.getAssociatedFileType();
        CharSequence fileSnippet = StringUtil.first(text, 10 * 1024, false);
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        return fileFactory.createFileFromText(
            PathUtil.makeFileName("a", fileType == null ? "" : fileType.getDefaultExtension()),
            language,
            fileSnippet
        );
    }

    
    @RequiredUIAccess
    public static String reformat(Project project, Language language, String text) {
        return LanguageInternal.getInstance().reformatScratch(project, language, text);
    }
}
