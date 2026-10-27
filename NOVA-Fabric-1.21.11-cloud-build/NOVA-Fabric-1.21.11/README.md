# NOVA — Fabric 1.21.11

This is a **client-side Fabric mod starter project**, not yet a finished release.

## Current state
- Fabric mod metadata and client entrypoint.
- Right Shift opens a dark purple/black NOVA menu.
- Minecraft/Fabric dependencies configured for 1.21.11 and Java 21.
- StorageFinder and Freecam are placeholders; their actual world rendering and detached camera behavior are not implemented in this starter build.

## Build in the cloud (no local Gradle installation)
This repository includes `.github/workflows/build.yml`. To use it, upload the extracted project to a GitHub repository (the GitHub website can do this without installing Git), then open the repository's **Actions** tab and run **Build NOVA**. The workflow installs Java 21 and Gradle 8.14.3 on a GitHub-hosted runner, builds the project, and uploads the JAR files as an artifact if compilation succeeds.

This cloud workflow has not been run yet. It still needs internet access from GitHub's runner to download Gradle dependencies, and a successful build is not guaranteed until the workflow completes.

## Build locally
With Java 21 and a compatible Gradle 8.x installation, run `gradle build`. If successful, the mod JAR is in `build/libs/` (the non-`-sources.jar` file).

## Install
Copy the built JAR into `.minecraft/mods`. Use Fabric Loader for Minecraft 1.21.11 and install a matching Fabric API too.

## Important
The build has not been run in this environment, so compilation is not verified. This is not yet the complete NOVA feature set.
