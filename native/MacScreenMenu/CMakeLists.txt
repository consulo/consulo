cmake_minimum_required(VERSION 3.21)
project(MacScreenMenu OBJC)

if (NOT APPLE)
    message(FATAL_ERROR "macOS only.")
endif ()

# Consulo builds native libraries per-architecture (see native/MacFileSystemNotifier).
# TARGET_X64 / TARGET_ARM64 are passed by the build harness.
if (TARGET_X64)
    set(FILE_NAME "macscreenmenu64")
    set(CMAKE_OSX_ARCHITECTURES "x86_64")
elseif (TARGET_ARM64)
    set(FILE_NAME "macscreenmenu-aarch64")
    set(CMAKE_OSX_ARCHITECTURES "arm64")
else ()
    message(FATAL_ERROR "Unsupported platform: define TARGET_X64 or TARGET_ARM64")
endif ()

set(CMAKE_C_STANDARD 11)
set(CMAKE_OSX_DEPLOYMENT_TARGET 10.12.2)

# JNI headers (native methods bound by java.lang.System.load in Menu/MenuItem)
set(JAVA_HOME $ENV{JAVA_HOME})
if (NOT JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME is not defined.")
endif ()
include_directories(${JAVA_HOME}/include ${JAVA_HOME}/include/darwin)

add_compile_options("-fmodules" "-flto=full" "-Wall" "-Wextra" "-Wpedantic" "-Wno-newline-eof" "-Wno-unused-parameter")

add_library(${FILE_NAME} SHARED
        MenuItem.h MenuItem.m
        Menu.h Menu.m
        CustomMenuItemView.h CustomMenuItemView.m)
