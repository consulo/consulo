/*
 * Copyright 2013-2025 consulo.io
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

package consulo.test.junit.impl.language;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.DebugUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.test.junit.impl.extension.ConsuloProjectLoader;
import consulo.test.junit.impl.extension.InjectingRecord;
import consulo.test.junit.impl.extension.NoParamDisplayNameGenerator;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.virtualFileSystem.light.TextLightVirtualFileBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-01-13
 */
@DisplayNameGeneration(NoParamDisplayNameGenerator.class)
@ExtendWith(ConsuloProjectLoader.class)
public abstract class SimpleParsingTest<TestContext> {
    public record Context(TestInfo testInfo, PsiFileFactory psiFileFactory) implements InjectingRecord {
    }

    private final String myDataPath;
    private final String myExtension;

    public SimpleParsingTest(@Nonnull String dataPath, @Nonnull String ext) {
        myDataPath = dataPath;
        myExtension = ext;
    }

    @Nonnull
    protected abstract LanguageFileType getFileType(@Nonnull Context context, @Nullable TestContext testContext);

    @Nonnull
    @RequiredReadAction
    protected PsiFile createFile(@Nonnull Context context,
                                 @Nullable TestContext testContext,
                                 @Nonnull String fileName,
                                 @Nonnull FileType fileType,
                                 @Nonnull String text) {
        LanguageVersion languageVersion = resolveLanguageVersion(context, testContext, fileType);

        return context.psiFileFactory().createFileFromText(fileName, languageVersion.getLanguage(), languageVersion, text);
    }

    protected void doTest(@Nonnull Context context, @Nullable TestContext testContext) throws Exception {
        //noinspection RequiredXAction
        doTestImpl(context, testContext);
    }

    @Nonnull
    protected String getFileName(@Nonnull Context context, @Nullable TestContext testContext) {
        TestInfo testInfo = context.testInfo();

        String testName = testInfo.getTestMethod().get().getName();

        return NoParamDisplayNameGenerator.getName(testName, false);
    }

    @RequiredReadAction
    protected final void doTestImpl(@Nonnull Context context, @Nullable TestContext testContext) throws Exception {
        PsiFile file = loadPsiFile(context, testContext);

        checkResult(context, testContext, file);
    }

    @Nonnull
    @RequiredReadAction
    protected PsiFile loadPsiFile(@Nonnull Context context, @Nullable TestContext testContext) throws  Exception {
        String fileName = getFileName(context, testContext);

        String sourceText = loadText(context, testContext, myExtension);

        PsiFile file = createFile(context,
            testContext,
            fileName + "." + myExtension,
            getFileType(context, testContext),
            sourceText
        );

        ensureParsed(file);

        FileViewProvider viewProvider = file.getViewProvider();
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        if (virtualFile instanceof TextLightVirtualFileBase textFile) {
            Assertions.assertEquals(sourceText, textFile.getContent(), "light virtual file text mismatch");
        }

        Assertions.assertEquals(sourceText, LoadTextUtil.loadText(file.getVirtualFile()), "virtual file text mismatch");
        Assertions.assertEquals(sourceText, viewProvider.getDocument().getText(), "doc text mismatch");
        Assertions.assertEquals(sourceText, file.getText(), "psi text mismatch");

        return file;
    }

    protected void checkResult(@Nonnull Context context, @Nullable TestContext testContext, final PsiFile file) throws Exception {
        doCheckResult(context, testContext, file, checkAllPsiRoots(), skipSpaces(), includeRanges());
    }

    private void doCheckResult(Context context,
                               TestContext testContext,
                               PsiFile file,
                               boolean checkAllPsiRoots,
                               boolean skipSpaces,
                               boolean printRanges) throws Exception {
        FileViewProvider provider = file.getViewProvider();
        Set<Language> languages = provider.getLanguages();

        if (!checkAllPsiRoots || languages.size() == 1) {
            String resultText = loadText(context, testContext, "txt");
            Assertions.assertEquals(resultText, toParseTreeText(file, skipSpaces, printRanges).trim());
            return;
        }

        for (Language language : languages) {
            PsiFile root = provider.getPsi(language);

            String resultText = loadText(context, testContext, language.getID() + ".txt");

            Assertions.assertEquals(resultText, toParseTreeText(root, skipSpaces, printRanges).trim());
        }
    }

    @Nonnull
    protected String loadText(Context context, TestContext testContext, String extension) throws Exception {
        TestInfo testInfo = context.testInfo();
        
        String testName = testInfo.getTestMethod().get().getName();

        String fileName = NoParamDisplayNameGenerator.getName(testName, false);

        String path = "/" + myDataPath + "/" + fileName + "." + extension;

        InputStream sourceStream = getClass().getResourceAsStream(path);
        if (sourceStream == null) {
            return "";
        }

        return FileUtil.loadTextAndClose(sourceStream, true);
    }

    public static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    @Nonnull
    @RequiredReadAction
    protected LanguageVersion resolveLanguageVersion(@Nonnull Context context,
                                                     @Nullable TestContext testContext,
                                                     @Nonnull FileType fileType) {
        if (fileType instanceof LanguageFileType) {
            return LanguageVersionUtil.findDefaultVersion(((LanguageFileType) fileType).getLanguage());
        }
        throw new IllegalArgumentException(fileType.getId() + " is not extends 'LanguageFileType'");
    }

    protected static String toParseTreeText(final PsiElement file, boolean skipSpaces, boolean printRanges) {
        return DebugUtil.psiToString(file, skipSpaces, printRanges);
    }

    protected boolean includeRanges() {
        return false;
    }

    protected boolean skipSpaces() {
        return false;
    }

    protected boolean checkAllPsiRoots() {
        return true;
    }
}
