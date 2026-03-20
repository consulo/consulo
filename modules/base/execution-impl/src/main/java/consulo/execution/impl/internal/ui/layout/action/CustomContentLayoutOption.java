package consulo.execution.impl.internal.ui.layout.action;

public interface CustomContentLayoutOption {
    boolean isEnabled();

    boolean isSelected();

    void select();

    
    String getDisplayName();
}
