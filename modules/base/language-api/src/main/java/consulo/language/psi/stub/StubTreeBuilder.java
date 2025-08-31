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
package consulo.language.psi.stub;

import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TreeBackedLighterAST;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StubTreeBuilder {
  private static final Key<Stub> stubElementKey = Key.create("stub.tree.for.file.content");

  private StubTreeBuilder() {
  }

  @Nullable
  public static Stub buildStubTree(FileContent inputData) {
    Stub data = inputData.getUserData(stubElementKey);
    if (data != null) return data;

    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (inputData) {
      data = inputData.getUserData(stubElementKey);
      if (data != null) return data;

      FileType fileType = inputData.getFileType();

      BinaryFileStubBuilder builder = BinaryFileStubBuilder.forFileType(fileType);
      if (builder != null) {
        data = builder.buildStubTree(inputData);
        if (data instanceof PsiFileStubImpl && !((PsiFileStubImpl)data).rootsAreSet()) {
          ((PsiFileStubImpl)data).setStubRoots(new PsiFileStub[]{(PsiFileStubImpl)data});
        }
      }
      else {
        CharSequence contentAsText = inputData.getContentAsText();
        PsiDependentFileContent fileContent = (PsiDependentFileContent)inputData;
        PsiFile psi = fileContent.getPsiFile();
        FileViewProvider viewProvider = psi.getViewProvider();
        psi = viewProvider.getStubBindingRoot();
        psi.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, contentAsText);

        // if we load AST, it should be easily gc-able. See PsiFileImpl.createTreeElementPointer()
        psi.getManager().startBatchFilesProcessingMode();

        try {
          IStubFileElementType stubFileElementType = ((PsiFileWithStubSupport)psi).getElementTypeForStubBuilder();
          if (stubFileElementType != null) {
            StubBuilder stubBuilder = stubFileElementType.getBuilder();
            if (stubBuilder instanceof LightStubBuilder) {
              LightStubBuilder.FORCED_AST.set(fileContent.getLighterAST());
            }
            data = stubBuilder.buildStubTree(psi);

            List<Pair<IStubFileElementType, PsiFile>> stubbedRoots = getStubbedRoots(viewProvider);
            List<PsiFileStub> stubs = new ArrayList<>(stubbedRoots.size());
            stubs.add((PsiFileStub)data);

            for (Pair<IStubFileElementType, PsiFile> stubbedRoot : stubbedRoots) {
              PsiFile secondaryPsi = stubbedRoot.second;
              if (psi == secondaryPsi) continue;
              StubBuilder stubbedRootBuilder = stubbedRoot.first.getBuilder();
              if (stubbedRootBuilder instanceof LightStubBuilder) {
                LightStubBuilder.FORCED_AST.set(new TreeBackedLighterAST(secondaryPsi.getNode()));
              }
              StubElement element = stubbedRootBuilder.buildStubTree(secondaryPsi);
              if (element instanceof PsiFileStub) {
                stubs.add((PsiFileStub)element);
              }
              ensureNormalizedOrder(element);
            }
            PsiFileStub[] stubsArray = stubs.toArray(PsiFileStub.EMPTY_ARRAY);
            for (PsiFileStub stub : stubsArray) {
              if (stub instanceof PsiFileStubImpl) {
                ((PsiFileStubImpl)stub).setStubRoots(stubsArray);
              }
            }
          }
        }
        finally {
          psi.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, null);
          psi.getManager().finishBatchFilesProcessingMode();
        }
      }

      ensureNormalizedOrder(data);
      inputData.putUserData(stubElementKey, data);
      return data;
    }
  }

  private static void ensureNormalizedOrder(Stub element) {
    if (element instanceof StubBase<?>) {
      ((StubBase)element).myStubList.finalizeLoadingStage();
    }
  }

  /**
   * Order is deterministic. First element matches {@link FileViewProvider#getStubBindingRoot()}
   */
  @Nonnull
  public static List<Pair<IStubFileElementType, PsiFile>> getStubbedRoots(@Nonnull FileViewProvider viewProvider) {
    List<Trinity<Language, IStubFileElementType, PsiFile>> roots = new SmartList<>();
    PsiFile stubBindingRoot = viewProvider.getStubBindingRoot();
    for (Language language : viewProvider.getLanguages()) {
      PsiFile file = viewProvider.getPsi(language);
      if (file instanceof PsiFileWithStubSupport) {
        IElementType type = ((PsiFileWithStubSupport)file).getElementTypeForStubBuilder();
        if (type != null) {
          roots.add(Trinity.create(language, (IStubFileElementType)type, file));
        }
      }
    }

    ContainerUtil.sort(roots, (o1, o2) -> {
      if (o1.third == stubBindingRoot) return o2.third == stubBindingRoot ? 0 : -1;
      else if (o2.third == stubBindingRoot) return 1;
      else return StringUtil.compare(o1.first.getID(), o2.first.getID(), false);
    });

    return ContainerUtil.map(roots, trinity -> Pair.create(trinity.second, trinity.third));
  }
}