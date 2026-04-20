// Loader binary — finds the highest platform build and dispatches to the
// matching LoaderLibrary (.dll/.dylib/.so) inside that build.
//
// `launchConsulo2` (Win) / `launchConsulo` (Mac/Linux) is looked up by name.
// Preserving those symbol names is mandatory for binary compatibility with
// builds already in the field (see PLAN.md § ABI Contract).

#![cfg_attr(target_os = "windows", windows_subsystem = "windows")]

use anyhow::{Context, Result, bail};
use consulo_loader_core::{build_discovery, logging, path_ext::PathExt, single_instance, ui, variant};
use log::debug;
use std::env;
use std::path::{Path, PathBuf};

fn main() {
    logging::init();
    match run() {
        Ok(code) => std::process::exit(code),
        Err(e) => {
            ui::show_error(!logging::is_debug(), e);
            std::process::exit(1);
        }
    }
}

fn run() -> Result<i32> {
    let exe = env::current_exe()
        .or_else(|_| {
            env::args()
                .next()
                .map(PathBuf::from)
                .ok_or_else(|| anyhow::anyhow!("no argv[0]"))
        })
        .context("cannot determine exe path")?
        .strip_ns_prefix()?;
    debug!("exe: {exe:?}");

    let app_home = find_app_home(&exe)?;
    debug!("app home: {app_home:?}");

    let build = build_discovery::resolve(&app_home)?;
    debug!("build: {:?}", build.path);

    let library_path = build.path.join("bin").join(library_file_name());
    debug!("library: {library_path:?}");

    if !single_instance::check_and_register(&exe)? {
        debug!("another instance owns this exe; forwarded cmdline and exiting");
        return Ok(0);
    }

    dispatch_to_library(&library_path, &build.path, &app_home)
}

/// Walk up from the exe, looking for a directory that contains `platform/`.
/// Caps at 5 parents (matches XPlatLauncher behavior).
fn find_app_home(exe: &Path) -> Result<PathBuf> {
    let mut p = exe.canonicalize()?.strip_ns_prefix()?;
    for _ in 0..5 {
        p = match p.parent() {
            Some(parent) => parent.to_path_buf(),
            None => break,
        };
        if p.join("platform").is_dir() {
            return Ok(p);
        }
    }
    bail!("could not find app home (no `platform/` dir) near {exe:?}")
}

// ===== Library filename per OS & variant =====

#[cfg(target_os = "windows")]
fn library_file_name() -> String {
    let suffix = variant::filename_suffix();
    format!("consulo{suffix}{}.dll", arch_suffix())
}

#[cfg(target_os = "windows")]
fn arch_suffix() -> &'static str {
    #[cfg(target_arch = "x86_64")]
    {
        "64"
    }
    #[cfg(target_arch = "aarch64")]
    {
        "-aarch64"
    }
}

#[cfg(target_os = "macos")]
fn library_file_name() -> String {
    format!("libconsulo{}.dylib", variant::filename_suffix())
}

#[cfg(target_os = "linux")]
fn library_file_name() -> String {
    format!("libconsulo{}.so", variant::filename_suffix())
}

// ===== Dispatch =====

/// Paths the Loader hands over to the LoaderLibrary.
struct DispatchPaths {
    properties_file: PathBuf,
    vm_options_file: PathBuf,
}

fn resolve_paths(app_home: &Path) -> Result<DispatchPaths> {
    let properties_file = app_home.join("consulo.properties");

    // vmoptions path differs by platform (matches the current C++/ObjC code):
    //   Windows: <exe>.vmoptions  (e.g. consulo64.exe.vmoptions)
    //   macOS:   <app>/Contents/consulo.vmoptions
    //   Linux:   <exe>.vmoptions  (e.g. consulo.vmoptions next to the binary)
    #[cfg(target_os = "macos")]
    let vm_options_file = app_home.join("Contents/consulo.vmoptions");

    #[cfg(target_os = "windows")]
    let vm_options_file = {
        let exe = env::current_exe()?;
        let name = exe
            .file_name()
            .ok_or_else(|| anyhow::anyhow!("exe has no file name"))?
            .to_string_lossy()
            .into_owned();
        exe.with_file_name(format!("{name}.vmoptions"))
    };

    #[cfg(target_os = "linux")]
    let vm_options_file = {
        let exe = env::current_exe()?;
        let name = exe
            .file_name()
            .ok_or_else(|| anyhow::anyhow!("exe has no file name"))?
            .to_string_lossy()
            .into_owned();
        exe.with_file_name(format!("{name}.vmoptions"))
    };

    Ok(DispatchPaths {
        properties_file,
        vm_options_file,
    })
}

#[cfg(target_os = "windows")]
fn dispatch_to_library(lib_path: &Path, build: &Path, app_home: &Path) -> Result<i32> {
    use libloading::Library;
    use std::ffi::{OsStr, c_int, c_void};
    use std::os::windows::ffi::OsStrExt;

    type LaunchConsulo2 = unsafe extern "C" fn(
        *mut c_void, *mut c_void, *mut u16, c_int, c_int, *mut *mut u16,
        *mut u16, *mut u16, *mut u16, *mut u16, *mut u16,
    ) -> c_int;

    let lib =
        unsafe { Library::new(lib_path) }.with_context(|| format!("LoadLibrary {lib_path:?}"))?;
    let sym: libloading::Symbol<'_, LaunchConsulo2> =
        unsafe { lib.get(b"launchConsulo2\0") }.context("GetProcAddress launchConsulo2")?;

    fn to_utf16(os: &OsStr) -> Vec<u16> {
        os.encode_wide().chain(std::iter::once(0)).collect()
    }

    let paths = resolve_paths(app_home)?;
    let module_file_w = to_utf16(lib_path.as_os_str());
    let working_dir_w = to_utf16(build.as_os_str());
    let properties_file_w = to_utf16(paths.properties_file.as_os_str());
    let vm_options_w = to_utf16(paths.vm_options_file.as_os_str());
    let app_home_w = to_utf16(app_home.as_os_str());

    // UTF-16 NUL-terminated argv, stable across the call.
    let argv_buffers: Vec<Vec<u16>> = env::args_os()
        .map(|s| s.encode_wide().chain(std::iter::once(0)).collect())
        .collect();
    let mut argv_ptrs: Vec<*mut u16> =
        argv_buffers.iter().map(|v| v.as_ptr() as *mut u16).collect();

    let rc = unsafe {
        sym(
            std::ptr::null_mut(), std::ptr::null_mut(), std::ptr::null_mut(), 0,
            argv_ptrs.len() as c_int, argv_ptrs.as_mut_ptr(),
            module_file_w.as_ptr() as *mut u16,
            working_dir_w.as_ptr() as *mut u16,
            properties_file_w.as_ptr() as *mut u16,
            vm_options_w.as_ptr() as *mut u16,
            app_home_w.as_ptr() as *mut u16,
        )
    };
    Ok(rc as i32)
}

#[cfg(target_os = "macos")]
fn dispatch_to_library(lib_path: &Path, build: &Path, app_home: &Path) -> Result<i32> {
    use core_foundation::base::TCFType;
    use core_foundation::string::{CFString, CFStringRef};
    use libloading::Library;
    use std::ffi::{CString, c_char, c_int, c_void};
    use std::os::unix::ffi::OsStrExt;

    type LaunchConsulo = unsafe extern "C" fn(
        c_int, *mut *mut c_char,
        *const c_void, *const c_void, *const c_void, *const c_void,
    ) -> c_int;

    let lib =
        unsafe { Library::new(lib_path) }.with_context(|| format!("dlopen {lib_path:?}"))?;
    let sym: libloading::Symbol<'_, LaunchConsulo> =
        unsafe { lib.get(b"launchConsulo\0") }.context("dlsym launchConsulo")?;

    let paths = resolve_paths(app_home)?;

    // CFStrings must outlive the JNI call.
    let build_cf = CFString::new(&build.to_string_lossy());
    let app_home_cf = CFString::new(&app_home.to_string_lossy());
    let props_cf = CFString::new(&paths.properties_file.to_string_lossy());
    let vmopts_cf = CFString::new(&paths.vm_options_file.to_string_lossy());

    let argv_buffers: Vec<CString> = env::args_os()
        .map(|s| CString::new(s.as_bytes()).unwrap_or_default())
        .collect();
    let mut argv_ptrs: Vec<*mut c_char> =
        argv_buffers.iter().map(|c| c.as_ptr() as *mut c_char).collect();

    let as_ref = |cf: &CFString| cf.as_concrete_TypeRef() as *const c_void;
    let rc = unsafe {
        sym(
            argv_ptrs.len() as c_int,
            argv_ptrs.as_mut_ptr(),
            as_ref(&build_cf),
            as_ref(&props_cf),
            as_ref(&vmopts_cf),
            as_ref(&app_home_cf),
        )
    };

    // Borrow check: keep CFStrings alive across the call above.
    drop((build_cf, app_home_cf, props_cf, vmopts_cf));
    drop(argv_buffers);

    Ok(rc as i32)
}

#[cfg(target_os = "linux")]
fn dispatch_to_library(lib_path: &Path, build: &Path, app_home: &Path) -> Result<i32> {
    use libloading::Library;
    use std::ffi::{CString, c_char, c_int};
    use std::os::unix::ffi::OsStrExt;

    type LaunchConsulo = unsafe extern "C" fn(
        c_int, *mut *mut c_char,
        *const c_char, *const c_char, *const c_char, *const c_char,
    ) -> c_int;

    let lib =
        unsafe { Library::new(lib_path) }.with_context(|| format!("dlopen {lib_path:?}"))?;
    let sym: libloading::Symbol<'_, LaunchConsulo> =
        unsafe { lib.get(b"launchConsulo\0") }.context("dlsym launchConsulo")?;

    let paths = resolve_paths(app_home)?;
    let build_c = CString::new(build.as_os_str().as_bytes())?;
    let app_home_c = CString::new(app_home.as_os_str().as_bytes())?;
    let props_c = CString::new(paths.properties_file.as_os_str().as_bytes())?;
    let vm_c = CString::new(paths.vm_options_file.as_os_str().as_bytes())?;

    let argv_buffers: Vec<CString> = env::args_os()
        .map(|s| CString::new(s.as_bytes()).unwrap_or_default())
        .collect();
    let mut argv_ptrs: Vec<*mut c_char> =
        argv_buffers.iter().map(|c| c.as_ptr() as *mut c_char).collect();

    let rc = unsafe {
        sym(
            argv_ptrs.len() as c_int,
            argv_ptrs.as_mut_ptr(),
            build_c.as_ptr(),
            props_c.as_ptr(),
            vm_c.as_ptr(),
            app_home_c.as_ptr(),
        )
    };
    Ok(rc as i32)
}
