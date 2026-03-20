package consulo.execution.impl.internal.ui.layout.action;

public interface ContentLayoutStateSettings {
    boolean isSelected();

    void setSelected(boolean state);

    
    String getDisplayName();

    void restore();

    boolean isEnabled();
}
