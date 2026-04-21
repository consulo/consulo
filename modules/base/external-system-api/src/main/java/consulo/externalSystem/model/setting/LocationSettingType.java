package consulo.externalSystem.model.setting;

import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Enumerates possible types of 'gradle home' location setting.
 *
 * @author Denis Zhdanov
 * @since 2011-09-02
 */
public enum LocationSettingType {
    /**
     * User hasn't defined gradle location but the IDE discovered it automatically.
     */
    DEDUCED(ExternalSystemLocalize::settingTypeLocationDeduced, "TextField.inactiveForeground", "nimbusDisabledText"),

    /**
     * User hasn't defined gradle location and the IDE was unable to discover it automatically.
     */
    UNKNOWN(ExternalSystemLocalize::settingTypeLocationUnknown),

    /**
     * User defined gradle location but it's incorrect.
     */
    EXPLICIT_INCORRECT(ExternalSystemLocalize::settingTypeLocationExplicitIncorrect),

    EXPLICIT_CORRECT(ExternalSystemLocalize::settingTypeLocationExplicitCorrect);

    private final Function<LocalizeValue, LocalizeValue> myDescriptionGenerator;

    private final Color myColor;

    LocationSettingType(Function<LocalizeValue, LocalizeValue> descriptionGenerator) {
        this(descriptionGenerator, "TextField.foreground");
    }

    LocationSettingType(Function<LocalizeValue, LocalizeValue> descriptionGenerator, String... colorKeys) {
        myDescriptionGenerator = descriptionGenerator;
        Color c = null;
        for (String key : colorKeys) {
            c = UIManager.getColor(key);
            if (c != null) {
                break;
            }
        }

        assert c != null : "Can't find color for keys " + Arrays.toString(colorKeys);
        myColor = c;
    }

    /**
     * @return human-readable description of the current setting type
     */
    public LocalizeValue getDescription(ProjectSystemId externalSystemId) {
        return myDescriptionGenerator.apply(externalSystemId.getDisplayName());
    }

    public Color getColor() {
        return myColor;
    }
}
