/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryContentRevision;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFilePatch;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.util.function.Supplier;

public class ApplyBinaryShelvedFilePatch extends ApplyFilePatchBase<ShelvedBinaryFilePatch> {
    public ApplyBinaryShelvedFilePatch(ShelvedBinaryFilePatch patch) {
        super(patch);
    }

    protected void applyCreate(Project project, VirtualFile newFile, CommitContext commitContext) throws IOException {
        applyChange(project, newFile, null, null);
    }

    protected Result applyChange(Project project, VirtualFile fileToPatch, FilePath pathBeforeRename, Supplier<CharSequence> baseContents)
        throws IOException {
        try {
            ContentRevision contentRevision = myPatch.getShelvedBinaryFile().createChange(project).getAfterRevision();
            if (contentRevision != null) {
                assert (contentRevision instanceof ShelvedBinaryContentRevision);
                byte[] binaryContent = ((ShelvedBinaryContentRevision) contentRevision).getBinaryContent();
                //it may be new empty binary file
                fileToPatch.setBinaryContent(binaryContent != null ? binaryContent : ArrayUtil.EMPTY_BYTE_ARRAY);
            }
        }
        catch (VcsException e) {
            LOG.error("Couldn't apply shelved binary patch", e);
            return new Result(ApplyPatchStatus.FAILURE) {

                @Override
                public ApplyPatchForBaseRevisionTexts getMergeData() {
                    return null;
                }
            };
        }
        return SUCCESS;
    }
}
