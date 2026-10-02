package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;

@ExtensionImpl(order = "last")
public class HyperlinkAnnotatorFactory implements AnnotatorFactory {
    @Override
    public Annotator createAnnotator() {
        return new HyperlinkAnnotator();
    }

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }
}
