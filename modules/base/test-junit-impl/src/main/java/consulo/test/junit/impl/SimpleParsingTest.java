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

package consulo.test.junit.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
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
import consulo.project.Project;
import consulo.test.light.LightApplicationBuilder;
import consulo.test.light.LightProjectBuilder;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.virtualFileSystem.light.TextLightVirtualFileBase;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-01-13
 */
@DisplayNameGeneration(NoParamDisplayNameGenerator.class)
public abstract class SimpleParsingTest {
    private final String myDataPath;
    private final String myExtension;

    private static Disposable ourGlobalDisposable;

    private static Application ourApplication;

    private static Project ourProject;

    public SimpleParsingTest(@Nonnull String dataPath, @Nonnull String ext) {
        myDataPath = dataPath;
        myExtension = ext;
    }

    @Nonnull
    protected abstract LanguageFileType getFileType();

    @Nonnull
    @RequiredReadAction
    protected PsiFile createFile(@Nonnull TestInfo testInfo,
                                 @Nonnull String fileName,
                                 @Nonnull FileType fileType,
                                 @Nonnull String text) {
        LanguageVersion languageVersion = resolveLanguageVersion(testInfo, fileType);

        PsiFileFactory fileFactory = PsiFileFactory.getInstance(ourProject);
        return fileFactory.createFileFromText(fileName, languageVersion.getLanguage(), languageVersion, text);
    }

    @BeforeAll
    public static void beforeAll() {
        if (ourGlobalDisposable != null) {
            throw new IllegalArgumentException("Duplicate Start");
        }

        ourGlobalDisposable = Disposable.newDisposable("App Disposable");

        LightApplicationBuilder builder = LightApplicationBuilder.create(ourGlobalDisposable);

        ourApplication = builder.build();

        LightProjectBuilder projectBuilder = LightProjectBuilder.create(ourApplication);

        ourProject = projectBuilder.build();
    }

    @AfterAll
    public static void afterAll() {
        if (ourGlobalDisposable == null) {
            throw new IllegalArgumentException("Duplicate Start");
        }

        Disposer.dispose(ourGlobalDisposable);
        ourGlobalDisposable = null;
        Disposer.assertIsEmpty();

        ourProject = null;
        ourApplication = null;
    }

    protected void doTest(TestInfo testInfo) throws Exception {
        //noinspection RequiredXAction
        doTestImpl(testInfo);
    }

    @RequiredReadAction
    protected void doTestImpl(TestInfo testInfo) throws Exception {
        String testName = testInfo.getTestMethod().get().getName();

        String fileName = NoParamDisplayNameGenerator.getName(testName, false);

        String sourceText = loadText(testInfo, myExtension);

        PsiFile file = createFile(testInfo, fileName + "." + myExtension, getFileType(), sourceText);

        ensureParsed(file);

        FileViewProvider viewProvider = file.getViewProvider();
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        if (virtualFile instanceof TextLightVirtualFileBase textFile) {
            Assertions.assertEquals(sourceText, textFile.getContent(), "light virtual file text mismatch");
        }

        Assertions.assertEquals(sourceText, LoadTextUtil.loadText(file.getVirtualFile()), "virtual file text mismatch");
        Assertions.assertEquals(sourceText, viewProvider.getDocument().getText(), "doc text mismatch");
        Assertions.assertEquals(sourceText, file.getText(), "psi text mismatch");

        checkResult(testInfo, file);
    }

    protected void checkResult(TestInfo testInfo, final PsiFile file) throws Exception {
        doCheckResult(testInfo, file, checkAllPsiRoots(), skipSpaces(), includeRanges());
    }

    private void doCheckResult(TestInfo testInfo,
                               PsiFile file,
                               boolean checkAllPsiRoots,
                               boolean skipSpaces,
                               boolean printRanges) throws Exception {
        FileViewProvider provider = file.getViewProvider();
        Set<Language> languages = provider.getLanguages();

        if (!checkAllPsiRoots || languages.size() == 1) {
            String resultText = loadText(testInfo, "txt");
            Assertions.assertEquals(resultText, toParseTreeText(file, skipSpaces, printRanges).trim());
            return;
        }

        for (Language language : languages) {
            PsiFile root = provider.getPsi(language);

            String resultText = loadText(testInfo, language.getID() + ".txt");

            Assertions.assertEquals(resultText, toParseTreeText(root, skipSpaces, printRanges).trim());
        }
    }

    @Nonnull
    private String loadText(TestInfo testInfo, String extension) throws Exception {
        String testName = testInfo.getTestMethod().get().getName();

        String fileName = NoParamDisplayNameGenerator.getName(testName, false);

        String path = "/" + myDataPath + "/" + fileName + "." + extension;

        InputStream sourceStream = getClass().getResourceAsStream(path);
        if (sourceStream == null) {
            throw new IllegalArgumentException("There no data for test path: " + path);
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
    protected LanguageVersion resolveLanguageVersion(@Nonnull TestInfo testInfo, @Nonnull FileType fileType) {
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
