/*
 * Copyright 2013-2016 consulo.io
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
package consulo.fileTypes;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 29.04.14
 */
public interface FileTypeWithPredefinedCharset extends FileType {
  @Nonnull
  Pair<Charset, String> getPredefinedCharset(@Nonnull VirtualFile virtualFile);
}
