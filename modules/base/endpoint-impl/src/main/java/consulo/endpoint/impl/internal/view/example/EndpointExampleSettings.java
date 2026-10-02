package consulo.endpoint.impl.internal.view.example;

import consulo.endpoint.client.generator.ClientGeneratorSetting;
import org.jspecify.annotations.Nullable;

record EndpointExampleSettings(boolean boilerplate, @Nullable String frameworkLanguage, @Nullable String frameworkVersion) {
    void applyTo(ClientGeneratorSetting setting) {
        setting.setBoilerplate(boilerplate);
        setting.setFrameworkLanguage(frameworkLanguage);
        setting.setFrameworkVersion(frameworkVersion);
    }
}
