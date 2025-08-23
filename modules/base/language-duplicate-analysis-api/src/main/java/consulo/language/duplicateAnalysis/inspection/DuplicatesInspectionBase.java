/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.duplicateAnalysis.inspection;

import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.content.FileIndex;
import consulo.document.util.TextRange;
import consulo.language.ast.FileASTNode;
import consulo.language.ast.LighterAST;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.TreeBackedLighterAST;
import consulo.language.duplicateAnalysis.DuplicatesProfile;
import consulo.language.duplicateAnalysis.DuplocateVisitor;
import consulo.language.duplicateAnalysis.DuplocatorState;
import consulo.language.duplicateAnalysis.LightDuplicateProfile;
import consulo.language.duplicateAnalysis.internal.DuplicatesIndex;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.ILightStubFileElementType;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.project.content.TestSourcesFilter;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntLongHashMap;
import gnu.trove.TIntObjectHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class DuplicatesInspectionBase extends LocalInspectionTool {
  public boolean myFilterOutGeneratedCode;
  private static final int MIN_FRAGMENT_SIZE = 3; // todo 3 statements constant

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@Nonnull PsiFile psiFile, @Nonnull InspectionManager manager, boolean isOnTheFly) {
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (!(virtualFile instanceof VirtualFileWithId) || /*!isOnTheFly || */!DuplicatesIndex.ourEnabled) return ProblemDescriptor.EMPTY_ARRAY;
    DuplicatesProfile profile = DuplicatesIndex.findDuplicatesProfile(psiFile.getFileType());
    if (profile == null) return ProblemDescriptor.EMPTY_ARRAY;


    FileASTNode node = psiFile.getNode();
    boolean usingLightProfile = profile instanceof LightDuplicateProfile &&
                                node.getElementType() instanceof ILightStubFileElementType &&
                                DuplicatesIndex.ourEnabledLightProfiles;
    Project project = psiFile.getProject();
    DuplicatedCodeProcessor<?> processor;
    if (usingLightProfile) {
      processor = processLightDuplicates(node, virtualFile, (LightDuplicateProfile)profile, project);
    }
    else {
      processor = processPsiDuplicates(psiFile, virtualFile, profile, project);
    }
    if (processor == null) return null;

    SmartList<ProblemDescriptor> descriptors = new SmartList<>();
    VirtualFile baseDir = project.getBaseDir();
    for (Map.Entry<Integer, TextRange> entry : processor.reportedRanges.entrySet()) {
      Integer offset = entry.getKey();
      if (!usingLightProfile && processor.fragmentSize.get(offset) < MIN_FRAGMENT_SIZE) continue;
      VirtualFile file = processor.reportedFiles.get(offset);
      String path = null;

      if (file.equals(virtualFile)) {
        path = "this file";
      }
      else if (baseDir != null) {
        path = VirtualFileUtil.getRelativePath(file, baseDir);
      }
      if (path == null) {
        path = file.getPath();
      }
      String message = "Found duplicated code in " + path;

      PsiElement targetElement = processor.reportedPsi.get(offset);
      TextRange rangeInElement = entry.getValue();
      int offsetInOtherFile = processor.reportedOffsetInOtherFiles.get(offset);

      LocalQuickFix fix = isOnTheFly ? createNavigateToDupeFix(file, offsetInOtherFile) : null;
      long hash = processor.fragmentHash.get(offset);

      int hash2 = (int)(hash >> 32);
      LocalQuickFix viewAllDupesFix = isOnTheFly && hash != 0 ? createShowOtherDupesFix(virtualFile, offset, (int)hash, hash2) : null;

      boolean onlyExtractable = Registry.is("duplicates.inspection.only.extractable");
      LocalQuickFix extractMethodFix =
        (isOnTheFly || onlyExtractable) && hash != 0 ? createExtractMethodFix(targetElement, rangeInElement, (int)hash, hash2) : null;
      if (onlyExtractable) {
        if (extractMethodFix == null) return null;
        if (!isOnTheFly) extractMethodFix = null;
      }

      ProblemDescriptor descriptor = manager
        .createProblemDescriptor(targetElement, rangeInElement, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly, fix,
                                 viewAllDupesFix, extractMethodFix);
      descriptors.add(descriptor);
    }

    return descriptors.isEmpty() ? null : descriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private DuplicatedCodeProcessor<?> processLightDuplicates(FileASTNode node,
                                                            VirtualFile virtualFile,
                                                            LightDuplicateProfile profile,
                                                            Project project) {
    Ref<DuplicatedCodeProcessor<LighterASTNode>> processorRef = new Ref<>();
    LighterAST lighterAST = node.getLighterAST();

    profile.process(lighterAST, (hash, hash2, ast, nodes) -> {
      DuplicatedCodeProcessor<LighterASTNode> processor = processorRef.get();
      if (processor == null) {
        processorRef.set(processor = new LightDuplicatedCodeProcessor((TreeBackedLighterAST)ast, virtualFile, project));
      }
      processor.process(hash, hash2, nodes[0]);
    });
    return processorRef.get();
  }

  private DuplicatedCodeProcessor<?> processPsiDuplicates(PsiFile psiFile,
                                                          VirtualFile virtualFile,
                                                          DuplicatesProfile profile,
                                                          Project project) {
    DuplocatorState state = profile.getDuplocatorState(psiFile.getLanguage());
    Ref<DuplicatedCodeProcessor<PsiFragment>> processorRef = new Ref<>();

    DuplocateVisitor visitor = profile.createVisitor((hash, cost, frag) -> {
      if (!DuplicatesIndex.isIndexedFragment(frag, cost, profile, state)) {
        return;
      }
      DuplicatedCodeProcessor<PsiFragment> processor = processorRef.get();
      if (processor == null) {
        processorRef.set(processor = new OldDuplicatedCodeProcessor(virtualFile, project));
      }
      processor.process(hash, 0, frag);
    }, true);

    visitor.visitNode(psiFile);
    return processorRef.get();
  }

  protected LocalQuickFix createNavigateToDupeFix(@Nonnull VirtualFile file, int offsetInOtherFile) {
    return null;
  }

  protected LocalQuickFix createShowOtherDupesFix(VirtualFile file, int offset, int hash, int hash2) {
    return null;
  }

  protected LocalQuickFix createExtractMethodFix(@Nonnull PsiElement targetElement,
                                                 @Nullable TextRange rangeInElement,
                                                 int hash,
                                                 int hash2) {
    return null;
  }

  private class LightDuplicatedCodeProcessor extends DuplicatedCodeProcessor<LighterASTNode> {
    private final TreeBackedLighterAST myAst;

    private LightDuplicatedCodeProcessor(@Nonnull TreeBackedLighterAST ast, VirtualFile file, Project project) {
      super(file, project, myFilterOutGeneratedCode);
      myAst = ast;
    }

    @Override
    protected TextRange getRangeInElement(LighterASTNode node) {
      return null;
    }

    @Override
    protected PsiElement getPsi(LighterASTNode node) {
      return myAst.unwrap(node).getPsi();
    }

    @Override
    protected int getStartOffset(LighterASTNode node) {
      return node.getStartOffset();
    }

    @Override
    protected int getEndOffset(LighterASTNode node) {
      return node.getEndOffset();
    }

    @Override
    protected boolean isLightProfile() {
      return true;
    }
  }

  class OldDuplicatedCodeProcessor extends DuplicatedCodeProcessor<PsiFragment> {
    private OldDuplicatedCodeProcessor(VirtualFile file, Project project) {
      super(file, project, myFilterOutGeneratedCode);
    }

    @Override
    protected TextRange getRangeInElement(PsiFragment node) {
      PsiElement[] elements = node.getElements();
      TextRange rangeInElement = null;
      if (elements.length > 1) {

        PsiElement lastElement = elements[elements.length - 1];
        rangeInElement = new TextRange(
          elements[0].getStartOffsetInParent(),
          lastElement.getStartOffsetInParent() + lastElement.getTextLength()
        );
      }
      return rangeInElement;
    }

    @Override
    protected PsiElement getPsi(PsiFragment node) {
      PsiElement[] elements = node.getElements();

      return elements.length > 1 ? elements[0].getParent() : elements[0];
    }

    @Override
    protected int getStartOffset(PsiFragment node) {
      return node.getStartOffset();
    }

    @Override
    protected int getEndOffset(PsiFragment node) {
      return node.getEndOffset();
    }

    @Override
    protected boolean isLightProfile() {
      return false;
    }
  }

  abstract static class DuplicatedCodeProcessor<T> implements FileBasedIndex.ValueProcessor<IntList> {
    final TreeMap<Integer, TextRange> reportedRanges = new TreeMap<>();
    final TIntObjectHashMap<VirtualFile> reportedFiles = new TIntObjectHashMap<>();
    final TIntObjectHashMap<PsiElement> reportedPsi = new TIntObjectHashMap<>();
    final TIntIntHashMap reportedOffsetInOtherFiles = new TIntIntHashMap();
    final TIntIntHashMap fragmentSize = new TIntIntHashMap();
    final TIntLongHashMap fragmentHash = new TIntLongHashMap();
    final VirtualFile virtualFile;
    final Project project;
    final FileIndex myFileIndex;
    final boolean mySkipGeneratedCode;
    final boolean myFileWithinGeneratedCode;
    T myNode;
    int myHash;
    int myHash2;

    DuplicatedCodeProcessor(VirtualFile file, Project project, boolean skipGeneratedCode) {
      virtualFile = file;
      this.project = project;
      myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      mySkipGeneratedCode = skipGeneratedCode;
      myFileWithinGeneratedCode = skipGeneratedCode && GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project);
    }

    void process(int hash, int hash2, T node) {
      ProgressManager.checkCanceled();
      myNode = node;
      myHash = hash;
      myHash2 = hash2;
      FileBasedIndex.getInstance().processValues(DuplicatesIndex.NAME, hash, null, this, GlobalSearchScope.projectScope(project));
    }

    @Override
    public boolean process(@Nonnull VirtualFile file, IntList list) {
      for(int i = 0, len = list.size(); i < len; i+=2) {
        ProgressManager.checkCanceled();

        if (list.get(i + 1) != myHash2) continue;
        int offset = list.get(i);

        if (myFileIndex.isInSourceContent(virtualFile)) {
          if (!myFileIndex.isInSourceContent(file)) return true;
          if (!TestSourcesFilter.isTestSources(virtualFile, project) && TestSourcesFilter.isTestSources(file, project)) return true;
          if (mySkipGeneratedCode) {
            if (!myFileWithinGeneratedCode && GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project)) return true;
          }
        } else if (myFileIndex.isInSourceContent(file)) {
          return true;
        }

        int startOffset = getStartOffset(myNode);
        int endOffset = getEndOffset(myNode);
        if (file.equals(virtualFile) && offset >= startOffset && offset < endOffset) continue;

        PsiElement target = getPsi(myNode);
        TextRange rangeInElement = getRangeInElement(myNode);

        Integer fragmentStartOffsetInteger = startOffset;
        SortedMap<Integer,TextRange> map = reportedRanges.subMap(fragmentStartOffsetInteger, endOffset);
        int newFragmentSize = !map.isEmpty() ? 0:1;

        Iterator<Integer> iterator = map.keySet().iterator();
        while(iterator.hasNext()) {
          Integer next = iterator.next();
          iterator.remove();
          reportedFiles.remove(next);
          reportedOffsetInOtherFiles.remove(next);
          reportedPsi.remove(next);
          newFragmentSize += fragmentSize.remove(next);
        }

        reportedRanges.put(fragmentStartOffsetInteger, rangeInElement);
        reportedFiles.put(fragmentStartOffsetInteger, file);
        reportedOffsetInOtherFiles.put(fragmentStartOffsetInteger, offset);
        reportedPsi.put(fragmentStartOffsetInteger, target);
        fragmentSize.put(fragmentStartOffsetInteger, newFragmentSize);
        if (newFragmentSize >= MIN_FRAGMENT_SIZE || isLightProfile()) {
          fragmentHash.put(fragmentStartOffsetInteger, (myHash & 0xFFFFFFFFL) | ((long)myHash2 << 32));
        }
        return false;
      }
      return true;
    }

    protected abstract TextRange getRangeInElement(T node);
    protected abstract PsiElement getPsi(T node);

    protected abstract int getStartOffset(T node);
    protected abstract int getEndOffset(T node);
    protected abstract boolean isLightProfile();
  }
}
