// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class ClientGeneratorSetting {
    private boolean myBoilerplate;
    private @Nullable String myFrameworkLanguage;
    private @Nullable String myFrameworkVersion;

    public ClientGeneratorSetting() {
        this(false, null, null);
    }

    public ClientGeneratorSetting(boolean boilerplate, @Nullable String frameworkLanguage, @Nullable String frameworkVersion) {
        myBoilerplate = boilerplate;
        myFrameworkLanguage = frameworkLanguage;
        myFrameworkVersion = frameworkVersion;
    }

    public boolean getBoilerplate() {
        return myBoilerplate;
    }

    public void setBoilerplate(boolean boilerplate) {
        myBoilerplate = boilerplate;
    }

    public @Nullable String getFrameworkLanguage() {
        return myFrameworkLanguage;
    }

    public void setFrameworkLanguage(@Nullable String frameworkLanguage) {
        myFrameworkLanguage = frameworkLanguage;
    }

    public @Nullable String getFrameworkVersion() {
        return myFrameworkVersion;
    }

    public void setFrameworkVersion(@Nullable String frameworkVersion) {
        myFrameworkVersion = frameworkVersion;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientGeneratorSetting that)) {
            return false;
        }
        return myBoilerplate == that.myBoilerplate
            && Objects.equals(myFrameworkLanguage, that.myFrameworkLanguage)
            && Objects.equals(myFrameworkVersion, that.myFrameworkVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myBoilerplate, myFrameworkLanguage, myFrameworkVersion);
    }

    @Override
    public String toString() {
        return "ClientGeneratorSetting(boilerplate=" + myBoilerplate +
            ", frameworkLanguage=" + myFrameworkLanguage +
            ", frameworkVersion=" + myFrameworkVersion + ")";
    }
}
