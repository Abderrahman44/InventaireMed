package org.abdat.inventory.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage {
    FR, EN
}

interface AppStrings {
    val appTitle: String
    val appSubtitle: String
    val mainNavigation: String
    val navOperations: String
    val navConfiguration: String
    val navInventory: String
    val navSuppliers: String
    val navOrders: String
    val navDeliveries: String
    val navSorties: String
    val navDecharges: String
    val navServices: String
    val navPersonnel: String
    val navConfig: String
    val navDashboard: String

    val searchPlaceholder: String
    val addArticle: String
    val editArticle: String
    val save: String
    val cancel: String
    val confirm: String
    val delete: String
    val archive: String
    val unarchive: String
    val markObsolete: String
    val print: String
    val exportPdf: String
    val actions: String
    val filter: String
    val all: String
    val active: String
    val archived: String
    val obsolete: String

    val codeArticle: String
    val barcodeReference: String
    val designation: String
    val packagingUnit: String
    val family: String
    val sousFamily: String
    val quantity: String
    val unitPrice: String
    val totalValue: String
    val expirationDate: String
    val status: String
    val justificationReason: String

    val statusInStock: String
    val statusLowStock: String
    val statusOutOfStock: String
    val statusArchived: String
    val statusObsolete: String
    val statusActive: String
    val statusReturned: String

    val settingsTitle: String
    val themeMode: String
    val themeLight: String
    val themeDark: String
    val language: String
    val langFrench: String
    val langEnglish: String
    val userProfile: String
    val fullName: String
    val phone: String
    val email: String
    val role: String
    val defaultMinQuantity: String
    val documentPrefixes: String

    val personnelTitle: String
    val addPerson: String
    val newDotation: String
    val returnEquipment: String
    val assignedEquipment: String

    val dechargesTitle: String
    val newDecharge: String
    val organizationName: String
    val donorName: String
    val receptionDate: String
    val zeroValuationNotice: String

    val backupTitle: String
    val backupSubtitle: String
    val exportBackup: String
    val exportBackupDesc: String
    val restoreBackup: String
    val restoreBackupDesc: String
    val lastBackup: String
    val lastRestore: String
    val never: String
    val confirmRestoreTitle: String
    val confirmRestoreMessage: String

    val consumptionTitle: String
    val consumptionSubtitle: String
    val filterDateFrom: String
    val filterDateTo: String
    val filterService: String
    val allServices: String
    val allFamilies: String
    val allSousFamilies: String
    val resetFilters: String
    val printReport: String
    val totalLines: String
    val totalUnitsConsumed: String
    val totalFinancialValue: String
    val detailViewTitle: String
    val voucherRef: String
    val destinationService: String
    val destinationLocal: String
    val applicant: String
    val internalReceiver: String
    val motive: String
    val beneficiaries: String
    val serviceHead: String
    val consumedArticles: String
    val close: String
    val noConsumptionFound: String
}

object FrStrings : AppStrings {
    override val appTitle = "InventaireMed"
    override val appSubtitle = "Gestion Industrielle & Médicale"
    override val mainNavigation = "MENU PRINCIPAL"
    override val navOperations = "OPÉRATIONS"
    override val navConfiguration = "CONFIGURATION"
    override val navInventory = "Gestion de Stock"
    override val navSuppliers = "Fournisseurs"
    override val navOrders = "Commandes"
    override val navDeliveries = "Livraisons"
    override val navSorties = "Sorties Matériel"
    override val navDecharges = "Dons & Donations"
    override val navServices = "Services & Locaux"
    override val navPersonnel = "Personnel"
    override val navConfig = "Configuration & Paramètres"
    override val navDashboard = "Consommation"

    override val searchPlaceholder = "Rechercher un article, code, lot..."
    override val addArticle = "+ Nouvel Article"
    override val editArticle = "Modifier la fiche article"
    override val save = "Enregistrer"
    override val cancel = "Annuler"
    override val confirm = "Confirmer"
    override val delete = "Supprimer"
    override val archive = "Archiver"
    override val unarchive = "Désarchiver"
    override val markObsolete = "Rendre Obsolète"
    override val print = "Imprimer"
    override val exportPdf = "Exporter PDF"
    override val actions = "Actions"
    override val filter = "Filtrer"
    override val all = "Tous"
    override val active = "Actifs"
    override val archived = "Archivés"
    override val obsolete = "Obsolètes"

    override val codeArticle = "Code Article"
    override val barcodeReference = "Réf. Code-barres Fabriquant"
    override val designation = "Désignation de l'Article"
    override val packagingUnit = "Unité de Conditionnement"
    override val family = "Famille Principale"
    override val sousFamily = "Sous-Famille"
    override val quantity = "Quantité en Stock"
    override val unitPrice = "Prix Unitaire (DZD)"
    override val totalValue = "Valeur Totale (DZD)"
    override val expirationDate = "Date de Péremption"
    override val status = "Statut"
    override val justificationReason = "Motif obligatoire de la modification"

    override val statusInStock = "EN STOCK"
    override val statusLowStock = "STOCK FAIBLE"
    override val statusOutOfStock = "RUPTURE"
    override val statusArchived = "ARCHIVÉ"
    override val statusObsolete = "OBSOLÈTE"
    override val statusActive = "ACTIF"
    override val statusReturned = "RESTITUÉ"

    override val settingsTitle = "Paramètres du Système & Préférences"
    override val themeMode = "Thème d'affichage"
    override val themeLight = "Mode Clair"
    override val themeDark = "Mode Sombre"
    override val language = "Langue d'interface"
    override val langFrench = "Français"
    override val langEnglish = "English"
    override val userProfile = "Profil Utilisateur"
    override val fullName = "Nom Complet"
    override val phone = "Téléphone"
    override val email = "Adresse Email"
    override val role = "Fonction / Rôle"
    override val defaultMinQuantity = "Seuil d'Alerte Minimum Global (Défaut)"
    override val documentPrefixes = "Préfixes Numérotation Documents"

    override val personnelTitle = "Répertoire du Personnel"
    override val addPerson = "+ Nouveau Collaborateur"
    override val newDotation = "Attribuer Matériel"
    override val returnEquipment = "Restituer au Stock"
    override val assignedEquipment = "Équipements et Actifs Attribués"

    override val dechargesTitle = "Dons, Donations & Papiers de Décharge"
    override val newDecharge = "+ Nouvelle Donation / Décharge"
    override val organizationName = "Organisme Donateur / Institution"
    override val donorName = "Représentant / Responsable"
    override val receptionDate = "Date de Réception"
    override val zeroValuationNotice = "Règle comptable : Prix unitaire et valeur totale strictement nuls (0.00 DZD) pour ne pas fausser le budget."

    override val backupTitle = "Sauvegarde & Données"
    override val backupSubtitle = "Exportation complète, restauration sécurisée et intégrité de la base de données"
    override val exportBackup = "Exporter une Sauvegarde"
    override val exportBackupDesc = "Créer une copie complète de la base de données SQLite contenant tous les articles, fournisseurs, commandes, réceptions, sorties et configurations."
    override val restoreBackup = "Importer & Restaurer"
    override val restoreBackupDesc = "Remplacer l'état actuel de l'application par une sauvegarde existante avec vérification d'intégrité et sauvegarde de sécurité automatique."
    override val lastBackup = "Dernière sauvegarde"
    override val lastRestore = "Dernière restauration"
    override val never = "Jamais"
    override val confirmRestoreTitle = "Confirmer la restauration de la base de données"
    override val confirmRestoreMessage = "Attention : Cette opération remplacera toutes les données actuelles de l'application par le contenu du fichier sélectionné. Une sauvegarde de sécurité automatique sera créée. Voulez-vous continuer ?"

    override val consumptionTitle = "Suivi de la Consommation Interne"
    override val consumptionSubtitle = "Analyse détaillée des articles consommés par service & valorisation financière"
    override val filterDateFrom = "Du"
    override val filterDateTo = "Au"
    override val filterService = "Service"
    override val allServices = "Tous les services"
    override val allFamilies = "Toutes les familles"
    override val allSousFamilies = "Toutes les sous-familles"
    override val resetFilters = "Réinitialiser les filtres"
    override val printReport = "Imprimer Rapport"
    override val totalLines = "Lignes consommées"
    override val totalUnitsConsumed = "Total Unités Consommées"
    override val totalFinancialValue = "Valeur Financière Totale"
    override val detailViewTitle = "Détails du Bon de Sortie"
    override val voucherRef = "Réf. Bon"
    override val destinationService = "Service Destinataire"
    override val destinationLocal = "Local / Emplacement"
    override val applicant = "Demandeur"
    override val internalReceiver = "Récepteur Interne"
    override val motive = "Motif de Sortie"
    override val beneficiaries = "Bénéficiaires"
    override val serviceHead = "Chef de Service"
    override val consumedArticles = "Articles Consommés"
    override val close = "Fermer"
    override val noConsumptionFound = "Aucune consommation trouvée pour les critères sélectionnés"
}

object EnStrings : AppStrings {
    override val appTitle = "InventaireMed"
    override val appSubtitle = "Industrial & Medical Inventory"
    override val mainNavigation = "MAIN MENU"
    override val navOperations = "OPERATIONS"
    override val navConfiguration = "CONFIGURATION"
    override val navInventory = "Inventory Master"
    override val navSuppliers = "Suppliers"
    override val navOrders = "Purchase Orders"
    override val navDeliveries = "Deliveries"
    override val navSorties = "Stock Issues"
    override val navDecharges = "Gifts & Donations"
    override val navServices = "Departments & Rooms"
    override val navPersonnel = "Personnel Directory"
    override val navConfig = "Settings & Config"
    override val navDashboard = "Consumption Tracking"

    override val searchPlaceholder = "Search article, code, barcode..."
    override val addArticle = "+ New Article"
    override val editArticle = "Edit Article Details"
    override val save = "Save"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val delete = "Delete"
    override val archive = "Archive"
    override val unarchive = "Restore from Archive"
    override val markObsolete = "Mark Obsolete"
    override val print = "Print"
    override val exportPdf = "Export PDF"
    override val actions = "Actions"
    override val filter = "Filter"
    override val all = "All"
    override val active = "Active"
    override val archived = "Archived"
    override val obsolete = "Obsolete"

    override val codeArticle = "Article Code"
    override val barcodeReference = "Manufacturer Barcode / Ref"
    override val designation = "Article Designation"
    override val packagingUnit = "Packaging Unit"
    override val family = "Primary Family"
    override val sousFamily = "Sub-Family"
    override val quantity = "Stock Quantity"
    override val unitPrice = "Unit Price (DZD)"
    override val totalValue = "Total Value (DZD)"
    override val expirationDate = "Expiration Date"
    override val status = "Status"
    override val justificationReason = "Mandatory change justification reason"

    override val statusInStock = "IN STOCK"
    override val statusLowStock = "LOW STOCK"
    override val statusOutOfStock = "OUT OF STOCK"
    override val statusArchived = "ARCHIVED"
    override val statusObsolete = "OBSOLETE"
    override val statusActive = "ACTIVE"
    override val statusReturned = "RETURNED"

    override val settingsTitle = "System Settings & User Preferences"
    override val themeMode = "Display Theme"
    override val themeLight = "Light Mode"
    override val themeDark = "Dark Mode"
    override val language = "Interface Language"
    override val langFrench = "Français"
    override val langEnglish = "English"
    override val userProfile = "User Profile"
    override val fullName = "Full Name"
    override val phone = "Phone Number"
    override val email = "Email Address"
    override val role = "Position / Role"
    override val defaultMinQuantity = "Global Default Alert Threshold"
    override val documentPrefixes = "Document Numbering Prefixes"

    override val personnelTitle = "Personnel Directory"
    override val addPerson = "+ New Employee"
    override val newDotation = "Assign Equipment"
    override val returnEquipment = "Return to Stock"
    override val assignedEquipment = "Allocated Equipment & Assets"

    override val dechargesTitle = "Gifts, Donations & Discharge Receipts"
    override val newDecharge = "+ New Donation Receipt"
    override val organizationName = "Donor Organization / Institution"
    override val donorName = "Representative / Contact"
    override val receptionDate = "Reception Date"
    override val zeroValuationNotice = "Accounting rule: Unit price and total valuation strictly 0.00 DZD to avoid inflating budget."

    override val backupTitle = "Backup & Data Management"
    override val backupSubtitle = "Full export, secure restore, and database integrity control"
    override val exportBackup = "Export Backup"
    override val exportBackupDesc = "Create a full snapshot of the SQLite database containing all articles, suppliers, orders, receipts, dispatches, and configurations."
    override val restoreBackup = "Import & Restore"
    override val restoreBackupDesc = "Replace current application data with an existing backup file, including integrity checks and automatic pre-restore safety copy."
    override val lastBackup = "Last Backup"
    override val lastRestore = "Last Restore"
    override val never = "Never"
    override val confirmRestoreTitle = "Confirm Database Restoration"
    override val confirmRestoreMessage = "Warning: This operation will replace all current application records with the contents of the selected file. An automatic safety snapshot will be created. Do you want to proceed?"

    override val consumptionTitle = "Internal Consumption Tracking"
    override val consumptionSubtitle = "Detailed analysis of consumed articles per department & financial valuation"
    override val filterDateFrom = "From"
    override val filterDateTo = "To"
    override val filterService = "Department"
    override val allServices = "All Services"
    override val allFamilies = "All Families"
    override val allSousFamilies = "All Sub-Families"
    override val resetFilters = "Reset Filters"
    override val printReport = "Print Report"
    override val totalLines = "Consumed Lines"
    override val totalUnitsConsumed = "Total Units Consumed"
    override val totalFinancialValue = "Total Financial Valuation"
    override val detailViewTitle = "Issue Voucher Details"
    override val voucherRef = "Voucher Ref"
    override val destinationService = "Beneficiary Department"
    override val destinationLocal = "Room / Location"
    override val applicant = "Applicant"
    override val internalReceiver = "Internal Receiver"
    override val motive = "Issue Motive"
    override val beneficiaries = "Beneficiaries"
    override val serviceHead = "Department Head"
    override val consumedArticles = "Consumed Articles"
    override val close = "Close"
    override val noConsumptionFound = "No consumption records found matching the active criteria"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { FrStrings }
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.LIGHT }

enum class ThemeMode {
    LIGHT, DARK
}
