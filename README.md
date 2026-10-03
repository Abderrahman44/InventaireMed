<p align="center">
  <img src="docs/logos/eph-el-attaf.jpg" alt="EPH El Attaf" height="130">
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="docs/logos/esi-sba.jpg" alt="ESI Sidi-Bel-Abbès" height="130">
</p>

<h1 align="center">InventaireMed</h1>

<p align="center"><em>Gestion de Stock — hospital inventory management, desktop application</em></p>

## About

**InventaireMed** is a desktop inventory and stock management application (*Gestion de Stock*) designed for hospitals and healthcare institutions. It was developed during an internship at the **EPH El Attaf** public hospital (Ministry of Health) as a project of the **École Supérieure en Informatique de Sidi-Bel-Abbès (ESI-SBA)**.

It centralizes the management of all hospital stock, from pharmaceuticals and medical supplies to IT equipment and office materials, and tracks the whole flow from supplier conventions and purchase orders, through deliveries, to internal issues and consumption by service. The interface is available in French and English, with light and dark themes.

> 📄 Full requirements: see the [**Software Requirements Specification**](./docs/SRS_FINAL.md) in the [`docs`](./docs) folder.

### Key features

- **Stock management**: articles organized by family and sub-family (managed in the configuration screen), with quantities, expiration dates and unit prices.
- **Suppliers & conventions**: supplier profiles and purchase agreements, with optional TVA (VAT) applied automatically to orders.
- **Orders (Commandes)**: order tracking with detail views showing delivered and partially pending lines.
- **Deliveries (Bons de livraison)** and **issue slips (Bons de sortie)**: incoming and outgoing stock movements with beneficiary, requester and receiver traceability.
- **Consumption tracking**: analytics of articles consumed by internal services, filterable by date, service and category, with printable reports.
- **Donations**: incoming stock with no unit price.
- **Personnel & services**: staff directory, services/premises and heads of service.
- **Audit log, exports & backups**: history of operations, report/document export and database backup.

### Screenshots

| Stock management | Consumption |
|---|---|
| ![Stock management](docs/screenshots/01-stock-management.png) | ![Consumption](docs/screenshots/02-consumption.png) |

| Suppliers & conventions | Orders |
|---|---|
| ![Suppliers and conventions](docs/screenshots/03-suppliers-conventions.png) | ![Orders](docs/screenshots/04-orders.png) |

| Deliveries | Issue notes |
|---|---|
| ![Deliveries](docs/screenshots/05-deliveries.png) | ![Issue notes](docs/screenshots/06-issue-notes.png) |

| Donations | Services & locals |
|---|---|
| ![Donations](docs/screenshots/07-donations.png) | ![Services and locals](docs/screenshots/08-services-locals.png) |

| Personnel | Configuration |
|---|---|
| ![Personnel](docs/screenshots/09-personnel.png) | ![Configuration](docs/screenshots/10-configuration.png) |

| User profile (dark mode) | Sensitive convention edit |
|---|---|
| ![User profile](docs/screenshots/11-user-profile-dark-mode.png) | ![Convention edit warning](docs/screenshots/12-convention-edit-warning.png) |

Each screen is described in detail in the [SRS](./docs/SRS_FINAL.md).

### Tech stack

- Kotlin Multiplatform and Compose Multiplatform (Material 3) for the UI, targeting Desktop (JVM)
- SQLDelight with SQLite for local persistence
- Koin for dependency injection

---

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