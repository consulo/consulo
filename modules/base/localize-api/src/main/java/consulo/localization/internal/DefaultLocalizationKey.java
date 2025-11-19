package consulo.localization.internal;

import consulo.localization.LocalizationManager;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2020-05-20
 */
public final class DefaultLocalizationKey implements LocalizeKey {
    @Nonnull
    private final LocalizationManager myLocalizationManager;
    @Nonnull
    private final String myLocalizationId;
    @Nonnull
    private final String myKey;

    private LocalizeValue myDefaultValue;

    public DefaultLocalizationKey(@Nonnull LocalizationManager manager, @Nonnull String localizationId, @Nonnull String key) {
        myLocalizationManager = manager;
        myLocalizationId = localizationId;
        myKey = key;
    }

    @Nonnull
    @Override
    public String getLocalizationId() {
        return myLocalizationId;
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

        myDefaultValue = defaultValue = new DefaultLocalizedValue(myLocalizationManager, this);
        return defaultValue;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(@Nonnull Object... args) {
        return new DefaultLocalizedValue(myLocalizationManager, this, args);
    }

    @Override
    public String toString() {
        return myLocalizationId + "@" + myKey;
    }
}
