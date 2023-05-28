/*
 * Copyright 2013-2023 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.pro.licensechecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * This is stub for Vaadin License Checker, since we will never use commercial libraries in opensource projects,
 *
 * and we don't want attach commercial libraries in opensource project
 */
public class LicenseChecker {
  public static Logger getLogger() {
    return LoggerFactory.getLogger(LicenseChecker.class);
  }

  public interface Callback {

    public void ok();

    public void failed(Exception e);
  }

  public static void setStrictOffline(boolean strictOffline) {
  }

  /**
   * @deprecated use {@link #checkLicense(String, String, BuildType)}
   */
  @Deprecated
  public static void checkLicense(String productName, String productVersion) {
  }

  /**
   * Checks the license for the given product version.
   *
   * @param productName    the name of the product to check
   * @param productVersion the version of the product to check
   * @param buildType      the type of build: development or production
   * @throws LicenseException if the license check fails
   */
  public static void checkLicense(String productName, String productVersion, BuildType buildType) {
  }

  /**
   * @deprecated use {@link #checkLicense(String, String, BuildType, Consumer)}
   */
  public static void checkLicense(String productName, String productVersion,
                                  Consumer<String> noKeyUrlHandler) {
  }

  /**
   * Checks the license for the given product version.
   *
   * @param productName     the name of the product to check
   * @param productVersion  the version of the product to check
   * @param buildType       the type of build: development or production
   * @param noKeyUrlHandler a handler that is invoked to open the vaadin.com URL
   *                        to download the key file. Used when no key file is
   *                        avialable.
   * @throws LicenseException if the license check fails
   */
  public static void checkLicense(String productName, String productVersion, BuildType buildType,
                                  Consumer<String> noKeyUrlHandler) {
  }

  /**
   * @deprecated use
   * {@link #checkLicenseAsync(String, String, BuildType, Callback)}
   */
  @Deprecated
  public static void checkLicenseAsync(String productName,
                                       String productVersion, Callback callback) {
    checkLicenseAsync(productName, productVersion, BuildType.DEVELOPMENT, callback);
  }

  /**
   * Checks the license for the given product version in the background and
   * invokes the callback when done.
   *
   * @param productName    the name of the product to check
   * @param productVersion the version of the product to check
   * @param buildType      the type of build: development or production
   * @param callback       the callback to invoke with the result
   */
  public static void checkLicenseAsync(String productName,
                                       String productVersion, BuildType buildType, Callback callback) {
  }

  /**
   * @deprecated use
   * {@link #checkLicenseAsync(String, String, BuildType, Callback, Consumer)}
   */
  @Deprecated
  public static void checkLicenseAsync(String productName,
                                       String productVersion, Callback callback, Consumer<String> noKeyUrlHandler) {
  }
}
