/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.patch.DefaultPatchBaseVersionProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ApplyPatchForBaseRevisionTexts {
    private final CharSequence myLocal;
    private CharSequence myBase;
    private String myPatched;
    private List<String> myWarnings;
    private boolean myBaseRevisionLoaded;

    @Nonnull
    public static ApplyPatchForBaseRevisionTexts create(
        Project project,
        VirtualFile file,
        FilePath pathBeforeRename,
        TextFilePatch patch,
        Supplier<CharSequence> baseContents
    ) {
        assert !patch.isNewFile();
        String beforeVersionId = patch.getBeforeVersionId();
        DefaultPatchBaseVersionProvider provider = null;
        if (beforeVersionId != null) {
            provider = new DefaultPatchBaseVersionProvider(project, file, beforeVersionId);
        }
        if (provider != null && provider.canProvideContent()) {
            return new ApplyPatchForBaseRevisionTexts(provider, pathBeforeRename, patch, file, baseContents);
        }
        else {
            return new ApplyPatchForBaseRevisionTexts(null, pathBeforeRename, patch, file, baseContents);
        }
    }

    private ApplyPatchForBaseRevisionTexts(
        DefaultPatchBaseVersionProvider provider,
        FilePath pathBeforeRename,
        TextFilePatch patch,
        VirtualFile file,
        Supplier<CharSequence> baseContents
    ) {
        myWarnings = new ArrayList<>();
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document != null) {
            fileDocumentManager.saveDocument(document);
        }
        myLocal = LoadTextUtil.loadText(file);

        List<PatchHunk> hunks = patch.getHunks();

        if (provider != null) {
            try {
                provider.getBaseVersionContent(
                    pathBeforeRename,
                    text -> {
                        GenericPatchApplier applier = new GenericPatchApplier(text, hunks);
                        if (!applier.execute()) {
                            return true;
                        }
                        myBase = text;
                        setPatched(applier.getAfter());
                        return false;
                    },
                    myWarnings
                );
            }
            catch (VcsException e) {
                myWarnings.add(e.getMessage());
            }
            myBaseRevisionLoaded = myPatched != null;
            if (myBaseRevisionLoaded) {
                return;
            }
        }

        CharSequence contents = baseContents.get();
        if (contents != null) {
            contents = StringUtil.convertLineSeparators(contents.toString());
            myBase = contents;
            myBaseRevisionLoaded = true;
            GenericPatchApplier applier = new GenericPatchApplier(contents, hunks);
            if (!applier.execute()) {
                applier.trySolveSomehow();
            }
            setPatched(applier.getAfter());
            return;
        }

        GenericPatchApplier applier = new GenericPatchApplier(myLocal, hunks);
        if (!applier.execute()) {
            applier.trySolveSomehow();
        }
        setPatched(applier.getAfter());
    }

    public CharSequence getLocal() {
        return myLocal;
    }

    public CharSequence getBase() {
        return myBase;
    }

    private void setPatched( String text) {
        myPatched = StringUtil.convertLineSeparators(text);
    }

    public String getPatched() {
        return myPatched;
    }

    public static String getCannotLoadBaseMessage( String filePatch) {
        return VcsLocalize.patchLoadBaseRevisionError(filePatch, "").get();
    }
}
