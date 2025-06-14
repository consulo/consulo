package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.language.Language;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.ui.Hyperlink;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;

public class ParameterHintsSettingsPanel extends JPanel {

    public ParameterHintsSettingsPanel(Language language, boolean excludeListSupported) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (excludeListSupported) {
            Hyperlink hyperlink = Hyperlink.create(LanguageEditorLocalize.settingsInlayJavaExcludeList().get());
            hyperlink.addHyperlinkListener(event -> {
                new ExcludeListDialog(language).show();
            });
            add(TargetAWT.to(hyperlink));
        }
    }
}
