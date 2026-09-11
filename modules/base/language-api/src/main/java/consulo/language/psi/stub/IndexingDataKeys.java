/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.language.ast.LighterAST;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;

import java.util.Map;

/**
 * @author yole
 */
public interface IndexingDataKeys {
  Key<VirtualFile> VIRTUAL_FILE = Key.create("Context virtual file");

  Key<PsiFile> PSI_FILE = Key.create("PSI for stubs");

  Key<CharSequence> FILE_TEXT_CONTENT_KEY = Key.create("file text content cached by stub indexer");

  Key<LighterAST> LIGHTER_AST_NODE_KEY = Key.create("lighter.ast.node");

  Key<Boolean> REBUILD_REQUESTED = Key.create("REBUILD_REQUESTED");

  /**
   * The module-aware option payloads, by provider id, that a PSI file was created to be parsed under. Set on a
   * {@link consulo.language.psi.stub.FileContent} before its PSI is built and copied onto that PSI, so the parse of a
   * secondary stub variant sees its own options rather than the file's recorded ones.
   */
  Key<Map<String, byte[]>> INDEX_OPTIONS = Key.create("module.aware.index.options");
}
