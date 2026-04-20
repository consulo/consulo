/*
 * Copyright 2013-2026 consulo.io
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
fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    // dev-fast: skip resource embedding in dev-build loop (saves ~2s on Windows).
    if std::env::var("CARGO_FEATURE_DEV_FAST").is_ok() {
        return;
    }

    let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if target_os != "windows" {
        return;
    }

    println!("cargo:rerun-if-changed=resources/consulo.ico");

    let mut res = winresource::WindowsResource::new();
    res.set("FileDescription", "Consulo");
    res.set("ProductName", "Consulo");
    res.set("CompanyName", "consulo.io");
    res.set("LegalCopyright", "Apache-2.0");
    res.set_icon("resources/consulo.ico");
    if let Err(e) = res.compile() {
        println!("cargo:warning=winresource compile failed: {e}");
    }
}
