package consulo.execution.impl.internal.ui.layout.action;

public interface CustomContentLayoutOptions {

    
    CustomContentLayoutOption[] getAvailableOptions();

    void select(CustomContentLayoutOption option);

    boolean isSelected(CustomContentLayoutOption option);

    boolean isHidden();

    void restore();

    void onHide();

    
    
    String getDisplayName();

    boolean isHideOptionVisible();
}
