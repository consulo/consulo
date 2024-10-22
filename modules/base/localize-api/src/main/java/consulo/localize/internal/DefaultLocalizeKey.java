package consulo.localize.internal;

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.localize.internal.DefaultLocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
public final class DefaultLocalizeKey implements LocalizeKey {
    private final String myLocalizeId;
    private final String myKey;

    private LocalizeValue myDefaultValue;

    public DefaultLocalizeKey(String localizeId, String key) {
        myLocalizeId = localizeId;
        myKey = key;
    }

    @Nonnull
    @Override
    public String getLocalizeId() {
        return myLocalizeId;
    }

    @Nonnull
    @Override
    public String getKey() {
        return myKey;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue() {
        LocalizeValue defaultValue = myDefaultValue;
        if (defaultValue != null) {
            return defaultValue;
        }

        defaultValue = new DefaultLocalizeValue(this);
        myDefaultValue = defaultValue;
        return defaultValue;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object arg) {
        return new DefaultLocalizeValue(this, arg);
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object arg0, Object arg1) {
        return new DefaultLocalizeValue(this, arg0, arg1);
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
        return new DefaultLocalizeValue(this, arg0, arg1, arg2);
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
        return new DefaultLocalizeValue(this, arg0, arg1, arg2, arg3);
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        return new DefaultLocalizeValue(this, arg0, arg1, arg2, arg3, arg4);
    }

    @Override
    public String toString() {
        return myLocalizeId + "@" + myKey;
    }
}
