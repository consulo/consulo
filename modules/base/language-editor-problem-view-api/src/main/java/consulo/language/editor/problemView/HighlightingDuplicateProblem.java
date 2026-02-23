// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.problemView;

/**
 * This is a marker interface used to filter out file problems
 * when similar problems are provided by highlighters.
 */
public interface HighlightingDuplicateProblem extends FileProblem {
}
