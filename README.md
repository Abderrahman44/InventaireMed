This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications. It contains
  several subfolders:
    - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name. For
      example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls. Similarly, if you want
      to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
      folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :shared:jvmTest`

### Releases & In-Place Upgrades

#### 1. Triggering a Release
You can publish a new release and build native desktop installers (`.msi`, `.exe`, `.deb`, `.rpm`) using either of two methods:

- **Via GitHub Actions (Recommended)**:
  1. Navigate to the **Actions** tab on GitHub $\rightarrow$ **Desktop CI/CD**.
  2. Click **Run workflow**.
  3. Select the branch (`main`) and choose the version bump (`patch`, `minor`, or `major`).
  4. The workflow will automatically compute the next version tag (e.g. `v1.0.0` $\rightarrow$ `v1.0.1`), tag the commit, build the installers, and publish the GitHub Release with all assets attached.

- **Via Git CLI**:
  ```bash
  git tag -a v1.0.1 -m "Release v1.0.1"
  git push origin v1.0.1
  ```

#### 2. How Windows In-Place Updates Work
The Windows distribution is configured with a permanent `upgradeUuid` in `desktopApp/build.gradle.kts`.
- When an end-user downloads a newer version (e.g., `v1.0.1.msi`) and double-clicks the installer, Windows Installer automatically performs a **Major Upgrade**:
  - Replaces older binaries with the new version in-place.
  - Updates Start Menu and Desktop shortcuts automatically.
  - **Does NOT** require uninstalling the old version first.
- **Database Safety**: The application's SQLite database is stored in the user profile directory (`~/.inventairemed/inventory.db`), meaning user inventory and transaction records are completely decoupled from the application binaries and persist safely across all version updates.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…