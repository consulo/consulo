// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2013-2026 consulo.io
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.CompletionResult;

import java.util.function.Consumer;

interface WeighingDelegate extends Consumer<CompletionResult> {
    void waitFor();
}
