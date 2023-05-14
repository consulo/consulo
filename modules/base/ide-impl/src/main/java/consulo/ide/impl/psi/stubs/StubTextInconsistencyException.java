/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.psi.stubs;

import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.PsiUtilCore;
import consulo.language.file.light.LightVirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.psi.stub.PsiFileStub;
import consulo.language.psi.stub.PsiFileStubImpl;
import consulo.language.psi.stub.StubTreeBuilder;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.ExceptionWithAttachments;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class StubTextInconsistencyException extends RuntimeException implements ExceptionWithAttachments {
  private final String myStubsFromText;
  private final String myStubsFromPsi;
  private final String myFileName;
  private final String myFileText;

  private StubTextInconsistencyException(String message, PsiFile file, List<PsiFileStub> fromText, List<PsiFileStub> fromPsi) {
    super(message);
    myStubsFromText = StringUtil.join(fromText, DebugUtil::stubTreeToString, "\n");
    myStubsFromPsi = StringUtil.join(fromPsi, DebugUtil::stubTreeToString, "\n");
    myFileName = file.getName();
    myFileText = file.getText();
  }

  @Nonnull
  public String getStubsFromText() {
    return myStubsFromText;
  }

  @Nonnull
  public String getStubsFromPsi() {
    return myStubsFromPsi;
  }

  @Nonnull
  @Override
  public Attachment[] getAttachments() {
    return new Attachment[]{AttachmentFactory.get().create(myFileName, myFileText), AttachmentFactory.get().create("stubsRestoredFromText.txt", myStubsFromText),
            AttachmentFactory.get().create("stubsFromExistingPsi.txt", myStubsFromPsi)};
  }

  public static void checkStubTextConsistency(@Nonnull PsiFile file) throws StubTextInconsistencyException {
    PsiUtilCore.ensureValid(file);

    FileViewProvider viewProvider = file.getViewProvider();
    if (viewProvider instanceof FreeThreadedFileViewProvider || viewProvider.getVirtualFile() instanceof LightVirtualFile) return;

    PsiFile bindingRoot = viewProvider.getStubBindingRoot();
    if (!(bindingRoot instanceof PsiFileImpl)) return;

    IStubFileElementType fileElementType = ((PsiFileImpl)bindingRoot).getElementTypeForStubBuilder();
    if (fileElementType == null || !fileElementType.shouldBuildStubFor(viewProvider.getVirtualFile())) return;

    List<PsiFileStub> fromText = restoreStubsFromText(viewProvider);

    List<PsiFileStub> fromPsi = ContainerUtil.map(StubTreeBuilder.getStubbedRoots(viewProvider), p -> ((PsiFileImpl)p.getSecond()).calcStubTree().getRoot());

    if (fromPsi.size() != fromText.size()) {
      throw new StubTextInconsistencyException(
              "Inconsistent stub roots: " + "PSI says it's " + ContainerUtil.map(fromPsi, s -> s.getType()) + " but re-parsing the text gives " + ContainerUtil.map(fromText, s -> s.getType()), file,
              fromText, fromPsi);
    }

    for (int i = 0; i < fromPsi.size(); i++) {
      PsiFileStub psiStub = fromPsi.get(i);
      if (!DebugUtil.stubTreeToString(psiStub).equals(DebugUtil.stubTreeToString(fromText.get(i)))) {
        throw new StubTextInconsistencyException("Stub is inconsistent with text in " + file.getLanguage(), file, fromText, fromPsi);
      }
    }
  }

  @Nonnull
  private static List<PsiFileStub> restoreStubsFromText(FileViewProvider viewProvider) {
    FileContentImpl fc = new FileContentImpl(viewProvider.getVirtualFile(), viewProvider.getContents(), 0);
    fc.setProject(viewProvider.getManager().getProject());
    PsiFileStubImpl copyTree = (PsiFileStubImpl)StubTreeBuilder.buildStubTree(fc);
    return copyTree == null ? Collections.emptyList() : Arrays.asList(copyTree.getStubRoots());
  }
}
