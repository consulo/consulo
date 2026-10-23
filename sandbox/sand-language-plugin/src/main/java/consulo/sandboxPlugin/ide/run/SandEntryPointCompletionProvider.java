package consulo.sandboxPlugin.ide.run;

import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;

import java.util.List;

class SandEntryPointCompletionProvider extends TextFieldCompletionProvider {
    @Override
    public void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result) {
        for (String variant : List.of("main", "mainTest", "benchmark")) {
            result.addElement(LookupElementBuilder.create(variant));
        }
    }
}
