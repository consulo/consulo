package consulo.endpoint.impl.internal.view.action;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class EndpointFilterItemToggleAction extends DumbAwareToggleAction {
    private final @Nullable Image myIcon;
    private final BooleanSupplier mySelected;
    private final Consumer<Boolean> mySetter;

    public EndpointFilterItemToggleAction(LocalizeValue text, @Nullable Image icon, BooleanSupplier selected, Consumer<Boolean> setter) {
        super(text, LocalizeValue.empty(), icon);
        myIcon = icon;
        mySelected = selected;
        mySetter = setter;
        getTemplatePresentation().setKeepPopupOnPerform(KeepPopupOnPerform.ALWAYS);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Image icon = myIcon;
        if (icon == null) {
            return;
        }
        Image checked = PlatformIconGroup.actionsChecked();
        Image mark = isSelected(e) ? checked : Image.empty(checked.getWidth(), checked.getHeight());
        e.getPresentation().setIcon(ImageEffects.appendRight(mark, icon));
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return mySelected.getAsBoolean();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        mySetter.accept(state);
    }
}
