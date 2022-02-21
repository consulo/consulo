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
package consulo.compiler;

import consulo.module.Module;
import consulo.project.Project;
import consulo.util.collection.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author nik
 */
public abstract class CompilerEncodingService {
  public static CompilerEncodingService getInstance(@Nonnull Project project) {
    return project.getInstance(CompilerEncodingService.class);
  }

  @Nullable
  public static Charset getPreferredModuleEncoding(Chunk<Module> chunk) {
    CompilerEncodingService service = null;
    for (Module module : chunk.getNodes()) {
      if (service == null) {
        service = getInstance(module.getProject());
      }
      final Charset charset = service.getPreferredModuleEncoding(module);
      if (charset != null) {
        return charset;
      }
    }
    return null;
  }

  @Nullable
  public abstract Charset getPreferredModuleEncoding(@Nonnull Module module);

  @Nonnull
  public abstract Collection<Charset> getAllModuleEncodings(@Nonnull Module module);
}
