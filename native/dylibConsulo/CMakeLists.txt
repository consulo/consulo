cmake_minimum_required(VERSION 3.1)
project(dylibConsulo OBJC)

if (NOT APPLE)
    message(FATAL_ERROR "macOS only.")
endif ()

if(TARGET_X64)
  set(FILE_NAME "consulo")
  set(CMAKE_OSX_ARCHITECTURES "x86_64")
elseif(TARGET_ARM64)
  set(FILE_NAME "consulo-aarch64")
    set(CMAKE_OSX_ARCHITECTURES "arm64")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

set(CMAKE_C_STANDARD 11)
set(CMAKE_OSX_DEPLOYMENT_TARGET 10.12)

add_compile_options("-fmodules" "-flto=full" "-Wall" "-Wextra" "-Wpedantic" "-Wno-newline-eof")

include_directories(JavaVM/include)

add_library(${FILE_NAME} SHARED Launcher.h Launcher.m main.m utils.h utils.m VMOptionsReader.h VMOptionsReader.m)

target_precompile_headers(${FILE_NAME} PUBLIC Launcher.pch)
