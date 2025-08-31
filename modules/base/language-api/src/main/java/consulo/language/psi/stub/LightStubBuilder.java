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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.language.ast.*;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.collection.BooleanStack;
import consulo.util.collection.Stack;
import consulo.util.collection.primitive.ints.IntStack;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import java.util.List;

public class LightStubBuilder implements StubBuilder {
  private static final Logger LOG = Logger.getInstance(LightStubBuilder.class);
  public static final ThreadLocal<LighterAST> FORCED_AST = new ThreadLocal<>();

  @RequiredReadAction
  @Override
  public StubElement buildStubTree(@Nonnull PsiFile file) {
    LighterAST tree = FORCED_AST.get();
    if (tree == null) {
      FileType fileType = file.getFileType();
      if (!(fileType instanceof LanguageFileType)) {
        LOG.error("File is not of LanguageFileType: " + file + ", " + fileType);
        return null;
      }
      if (!(file instanceof PsiFileWithStubSupport)) {
        LOG.error("Unexpected PsiFile instance: " + file + ", " + file.getClass());
        return null;
      }
      if (((PsiFileWithStubSupport)file).getElementTypeForStubBuilder() == null) {
        LOG.error("File is not of IStubFileElementType: " + file);
        return null;
      }

      FileASTNode node = file.getNode();
      tree = node.getElementType() instanceof ILightStubFileElementType ? node.getLighterAST() : new TreeBackedLighterAST(node);
    }
    else {
      FORCED_AST.set(null);
    }

    StubElement rootStub = createStubForFile(file, tree);
    buildStubTree(tree, tree.getRoot(), rootStub);
    return rootStub;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  protected StubElement createStubForFile(@Nonnull PsiFile file, @Nonnull LighterAST tree) {
    return new PsiFileStubImpl(file);
  }

  protected void buildStubTree(@Nonnull LighterAST tree, @Nonnull LighterASTNode root, @Nonnull StubElement rootStub) {
    Stack<LighterASTNode> parents = new Stack<>();
    IntStack childNumbers = new IntStack();
    BooleanStack parentsStubbed = new BooleanStack();
    Stack<List<LighterASTNode>> kinderGarden = new Stack<>();
    Stack<StubElement> parentStubs = new Stack<>();

    LighterASTNode parent = null;
    LighterASTNode element = root;
    List<LighterASTNode> children = null;
    int childNumber = 0;
    StubElement parentStub = rootStub;
    boolean immediateParentStubbed = true;

    nextElement:
    while (element != null) {
      ProgressManager.checkCanceled();

      StubElement stub = createStub(tree, element, parentStub);
      boolean hasStub = stub != parentStub || parent == null;
      if (hasStub && !immediateParentStubbed) {
        ((ObjectStubBase) stub).markDangling();
      }

      if (parent == null || !skipNode(tree, parent, element)) {
        List<LighterASTNode> kids = tree.getChildren(element);
        if (!kids.isEmpty()) {
          if (parent != null) {
            parents.push(parent);
            childNumbers.push(childNumber);
            kinderGarden.push(children);
            parentStubs.push(parentStub);
            parentsStubbed.push(immediateParentStubbed);
          }
          parent = element;
          immediateParentStubbed = hasStub;
          element = (children = kids).get(childNumber = 0);
          parentStub = stub;
          if (!skipNode(tree, parent, element)) continue nextElement;
        }
      }

      while (children != null && ++childNumber < children.size()) {
        element = children.get(childNumber);
        if (!skipNode(tree, parent, element)) continue nextElement;
      }

      element = null;
      while (!parents.isEmpty()) {
        parent = parents.pop();
        childNumber = childNumbers.pop();
        children = kinderGarden.pop();
        parentStub = parentStubs.pop();
        immediateParentStubbed = parentsStubbed.pop();
        while (++childNumber < children.size()) {
          element = children.get(childNumber);
          if (!skipNode(tree, parent, element)) continue nextElement;
        }
        element = null;
      }
    }
  }

  @Nonnull
  private static StubElement createStub(LighterAST tree, LighterASTNode element, StubElement parentStub) {
    IElementType elementType = element.getTokenType();
    if (elementType instanceof IStubElementType) {
      if (elementType instanceof ILightStubElementType) {
        ILightStubElementType lightElementType = (ILightStubElementType)elementType;
        if (lightElementType.shouldCreateStub(tree, element, parentStub)) {
          return lightElementType.createStub(tree, element, parentStub);
        }
      }
      else {
        LOG.error("Element is not of ILightStubElementType: " + ObjectUtil.objectInfo(elementType) + ", " + element);
      }
    }

    return parentStub;
  }

  private boolean skipNode(@Nonnull LighterAST tree, @Nonnull LighterASTNode parent, @Nonnull LighterASTNode node) {
    if (tree instanceof TreeBackedLighterAST) {
      return skipChildProcessingWhenBuildingStubs(((TreeBackedLighterAST)tree).unwrap(parent), ((TreeBackedLighterAST)tree).unwrap(node));
    }
    else {
      return skipChildProcessingWhenBuildingStubs(tree, parent, node);
    }
  }

  /**
   * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(LighterAST, LighterASTNode, LighterASTNode)}.
   */
  @Override
  public boolean skipChildProcessingWhenBuildingStubs(@Nonnull ASTNode parent, @Nonnull ASTNode node) {
    return false;
  }

  /**
   * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(ASTNode, ASTNode)}.
   */
  protected boolean skipChildProcessingWhenBuildingStubs(@Nonnull LighterAST tree, @Nonnull LighterASTNode parent, @Nonnull LighterASTNode node) {
    return false;
  }
}