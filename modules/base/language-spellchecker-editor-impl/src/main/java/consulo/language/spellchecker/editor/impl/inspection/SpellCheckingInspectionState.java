package consulo.language.spellchecker.editor.impl.inspection;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.util.xml.serializer.XmlSerializerUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-03-11
 */
public class SpellCheckingInspectionState implements InspectionToolState<SpellCheckingInspectionState> {
    public boolean processCode = true;
    public boolean processLiterals = true;
    public boolean processComments = true;

    @Nullable
    @Override
    public SpellCheckingInspectionState getState() {
        return this;
    }

    @Override
    public void loadState(SpellCheckingInspectionState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Nullable
    @Override
    public UnnamedConfigurable createConfigurable() {
        ConfigurableBuilder builder = ConfigurableBuilder.newBuilder();
        builder.checkBox(SpellCheckerLocalize.processCode(), () -> processCode, it -> processCode = it);
        builder.checkBox(
            SpellCheckerLocalize.processLiterals(),
            () -> processLiterals,
            it -> processLiterals = it
        );
        builder.checkBox(
            SpellCheckerLocalize.processComments(),
            () -> processComments,
            it -> processComments = it
        );

        return builder.buildUnnamed();
    }
}
