// Copyright 2000-2016 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff;

import consulo.application.progress.ProgressIndicator;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.LineFragment;

import java.util.List;

/**
 * Computes the changes of a diff request in place of the default comparison - see {@link DiffUserDataKeys#CUSTOM_DIFF_COMPUTER}.
 */
public interface DiffComputer {
    List<LineFragment> compute(CharSequence text1,
                               CharSequence text2,
                               ComparisonPolicy policy,
                               boolean innerChanges,
                               ProgressIndicator indicator);
}
