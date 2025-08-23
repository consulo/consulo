/**
 * @author VISTALL
 * @since 2025-08-23
 */
module consulo.language.duplicate.analysis.api {
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.usage.api;

    exports consulo.language.duplicateAnalysis;
    exports consulo.language.duplicateAnalysis.localize;
    exports consulo.language.duplicateAnalysis.equivalence;
    exports consulo.language.duplicateAnalysis.inspection;
    exports consulo.language.duplicateAnalysis.internal;
    exports consulo.language.duplicateAnalysis.iterator;
    exports consulo.language.duplicateAnalysis.treeHash;
    exports consulo.language.duplicateAnalysis.util;

    // TODO migrate to fastutil
    requires gnu.trove;
}