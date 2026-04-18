/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.lang.psi.stub;

import consulo.index.io.StringRef;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementTypeAsPsiFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.*;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 09-Jul-22
 */
public class SandClassStubElementType extends IStubElementType<SandClassStub, SandClass> implements IElementTypeAsPsiFactory {
  public SandClassStubElementType() {
    super("CLASS", SandLanguage.INSTANCE);
  }

  
  @Override
  public String getExternalId() {
    return "sand." + toString();
  }

  @Override
  public void serialize(SandClassStub stub, StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @Override
  public SandClassStub deserialize(StubInputStream dataStream, @Nullable StubElement parentStub) throws IOException {
    StringRef name = dataStream.readName();
    return new SandClassStub(parentStub, this, name);
  }

  @Override
  public void indexStub(SandClassStub stub, IndexSink sink) {
    String name = stub.getName();
    if (name == null) {
      return;
    }
    sink.occurrence(SandIndexKeys.SAND_CLASSES, name);
  }

  @Override
  public SandClass createPsi(SandClassStub stub) {
    return new SandClass(stub, this);
  }

  
  @Override
  public SandClassStub createStub(SandClass psi, StubElement parentStub) {
    return new SandClassStub(parentStub, this, psi.getName());
  }

  
  @Override
  public PsiElement createElement(ASTNode astNode) {
    return new SandClass(astNode);
  }
}