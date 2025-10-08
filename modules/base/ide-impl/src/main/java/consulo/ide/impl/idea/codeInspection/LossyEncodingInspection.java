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
package consulo.ide.impl.idea.codeInspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bootstrap.charset.Native2AsciiCharset;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.openapi.vfs.encoding.ChangeFileEncodingAction;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingUtil;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @author cdr
 * @since 2007-08-06
 */
@ExtensionImpl
public class LossyEncodingInspection extends LocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(LossyEncodingInspection.class);

    private static final LocalQuickFix CHANGE_ENCODING_FIX = new ChangeEncodingFix();
    private static final LocalQuickFix RELOAD_ENCODING_FIX = new ReloadInAnotherEncodingFix();

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return InspectionLocalize.groupNamesInternationalizationIssues();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return InspectionLocalize.lossyEncoding();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "LossyEncoding";
    }

    @Nullable
    @Override
    @RequiredReadAction
    public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
        if (InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
            return null;
        }
        if (!file.isPhysical()) {
            return null;
        }
        FileViewProvider viewProvider = file.getViewProvider();
        if (viewProvider.getBaseLanguage() != file.getLanguage()) {
            return null;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        if (!virtualFile.isInLocalFileSystem()) {
            return null;
        }
        CharSequence text = viewProvider.getContents();
        Charset charset = LoadTextUtil.extractCharsetFromFileContent(file.getProject(), virtualFile, text);

        // no sense in checking transparently decoded file: all characters there are already safely encoded
        if (charset instanceof Native2AsciiCharset) {
            return null;
        }

        List<ProblemDescriptor> descriptors = new SmartList<>();
        boolean ok = checkFileLoadedInWrongEncoding(file, manager, isOnTheFly, virtualFile, charset, descriptors);
        if (ok) {
            checkIfCharactersWillBeLostAfterSave(file, manager, isOnTheFly, text, charset, descriptors);
        }

        return descriptors.toArray(new ProblemDescriptor[descriptors.size()]);
    }

    private static boolean checkFileLoadedInWrongEncoding(
        @Nonnull PsiFile file,
        @Nonnull InspectionManager manager,
        boolean isOnTheFly,
        @Nonnull VirtualFile virtualFile,
        @Nonnull Charset charset,
        @Nonnull List<ProblemDescriptor> descriptors
    ) {
        if (FileDocumentManager.getInstance().isFileModified(virtualFile) // when file is modified, it's too late to reload it
            || !EncodingUtil.canReload(virtualFile) // can't reload in another encoding, no point trying
        ) {
            return true;
        }
        if (!isGoodCharset(virtualFile, charset)) {
            descriptors.add(manager.createProblemDescriptor(
                file,
                "File was loaded in the wrong encoding: '" + charset + "'",
                RELOAD_ENCODING_FIX,
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly
            ));
            return false;
        }
        return true;
    }

    // check if file was loaded in correct encoding
    // returns true if text converted with charset is equals to the bytes currently on disk
    private static boolean isGoodCharset(@Nonnull VirtualFile virtualFile, @Nonnull Charset charset) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (document == null) {
            return true;
        }
        byte[] loadedBytes;
        byte[] bytesToSave;
        try {
            loadedBytes = virtualFile.contentsToByteArray();
            bytesToSave = new String(loadedBytes, charset).getBytes(charset);
        }
        catch (Exception e) {
            return true;
        }
        byte[] bom = virtualFile.getBOM();
        if (bom != null && !ArrayUtil.startsWith(bytesToSave, bom)) {
            bytesToSave = ArrayUtil.mergeArrays(bom, bytesToSave); // for 2-byte encodings String.getBytes(Charset) adds BOM automatically
        }

        boolean equals = Arrays.equals(bytesToSave, loadedBytes);
        if (!equals && LOG.isDebugEnabled()) {
            try {
                FileUtil.writeToFile(new File("C:\\temp\\bytesToSave"), bytesToSave);
                FileUtil.writeToFile(new File("C:\\temp\\loadedBytes"), loadedBytes);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return equals;
    }

    private static void checkIfCharactersWillBeLostAfterSave(
        @Nonnull PsiFile file,
        @Nonnull InspectionManager manager,
        boolean isOnTheFly,
        @Nonnull CharSequence text,
        @Nonnull Charset charset,
        @Nonnull List<ProblemDescriptor> descriptors
    ) {
        int errorCount = 0;
        int start = -1;
        for (int i = 0; i <= text.length(); i++) {
            char c = i == text.length() ? 0 : text.charAt(i);
            if (i == text.length() || isRepresentable(c, charset)) {
                if (start != -1) {
                    TextRange range = new TextRange(start, i);
                    LocalizeValue message = InspectionLocalize.unsupportedCharacterForTheCharset(charset);
                    ProblemDescriptor descriptor = manager.createProblemDescriptor(
                        file,
                        range,
                        message.get(),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        CHANGE_ENCODING_FIX
                    );
                    descriptors.add(descriptor);
                    start = -1;
                    //do not report too many errors
                    if (errorCount++ > 200) {
                        break;
                    }
                }
            }
            else if (start == -1) {
                start = i;
            }
        }
    }

    private static boolean isRepresentable(char c, @Nonnull Charset charset) {
        String str = Character.toString(c);
        ByteBuffer out = charset.encode(str);
        CharBuffer buffer = charset.decode(out);
        return str.equals(buffer.toString());
    }

    private static class ReloadInAnotherEncodingFix extends ChangeEncodingFix {
        @Nonnull
        @Override
        public LocalizeValue getName() {
            return LocalizeValue.localizeTODO("Reload in another encoding");
        }

        @Override
        @RequiredReadAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            if (FileDocumentManager.getInstance().isFileModified(descriptor.getPsiElement().getContainingFile().getVirtualFile())) {
                return;
            }
            super.applyFix(project, descriptor);
        }
    }

    private static class ChangeEncodingFix implements LocalQuickFix {
        @Nonnull
        @Override
        public LocalizeValue getName() {
            return LocalizeValue.localizeTODO("Change file encoding");
        }

        @Override
        @RequiredReadAction
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
            VirtualFile virtualFile = psiFile.getVirtualFile();

            Editor editor = PsiUtilBase.findEditor(psiFile);
            DataContext dataContext = createDataContext(editor, editor == null ? null : editor.getComponent(), virtualFile, project);
            ListPopup popup = new ChangeFileEncodingAction(project.getApplication()).createPopup(dataContext);
            if (popup != null) {
                popup.showInBestPositionFor(dataContext);
            }
        }

        @Nonnull
        public static DataContext createDataContext(Editor editor, Component component, VirtualFile selectedFile, Project project) {
            DataContext parent = DataManager.getInstance().getDataContext(component);
            DataContext context =
                SimpleDataContext.getSimpleContext(UIExAWTDataKey.CONTEXT_COMPONENT, editor == null ? null : editor.getComponent(), parent);
            DataContext projectContext = SimpleDataContext.getSimpleContext(Project.KEY, project, context);
            return SimpleDataContext.getSimpleContext(VirtualFile.KEY, selectedFile, projectContext);
        }
    }
}
