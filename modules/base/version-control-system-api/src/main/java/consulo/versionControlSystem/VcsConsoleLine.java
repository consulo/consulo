// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem;

import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public final class VcsConsoleLine {
  private final List<Pair<String, ConsoleViewContentType>> myChunks;

  private VcsConsoleLine(@Nonnull List<Pair<String, ConsoleViewContentType>> chunks) {
    myChunks = chunks;
  }

  public void print(@Nonnull ConsoleView console) {
    ConsoleViewContentType lastType = ConsoleViewContentType.NORMAL_OUTPUT;
    for (Pair<String, ConsoleViewContentType> chunk : myChunks) {
      console.print(chunk.first, chunk.second);
      lastType = chunk.second;
    }
    console.print("\n", lastType);
  }

  @Nullable
  public static VcsConsoleLine create(@Nullable String message, @Nonnull ConsoleViewContentType contentType) {
    return create(Collections.singletonList(Pair.create(message, contentType)));
  }

  @Nullable
  public static VcsConsoleLine create(@Nonnull List<Pair<String, ConsoleViewContentType>> lineChunks) {
    List<Pair<String, ConsoleViewContentType>> chunks = ContainerUtil.filter(lineChunks, it -> !StringUtil.isEmptyOrSpaces(it.first));
    if (chunks.isEmpty()) return null;
    return new VcsConsoleLine(chunks);
  }
}
