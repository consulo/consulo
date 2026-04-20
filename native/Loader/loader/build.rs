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
