package org.abdat.inventory.data.mapper

import org.abdat.inventory.database.Fournisseur as DbFournisseur
import org.abdat.inventory.database.Convention as DbConvention
import org.abdat.inventory.database.Convention_item as DbConventionItem
import org.abdat.inventory.database.Service as DbService
import org.abdat.inventory.database.Local as DbLocal
import org.abdat.inventory.database.Stock_article as DbStockArticle
import org.abdat.inventory.database.Article_audit_log as DbArticleAuditLog
import org.abdat.inventory.database.Person as DbPerson
import org.abdat.inventory.database.Equipment_assignment as DbEquipmentAssignment
import org.abdat.inventory.database.Dynamic_config_item as DbDynamicConfigItem
import org.abdat.inventory.database.User_profile as DbUserProfile
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.ConventionItem
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.Local
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.domain.model.ArticleAuditLog
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.EquipmentAssignment
import org.abdat.inventory.domain.model.DynamicConfigItem
import org.abdat.inventory.domain.model.UserProfile

fun DbFournisseur.toDomain(): Fournisseur = Fournisseur(
    id = id,
    fullName = full_name,
    societe = societe,
    phone = phone,
    responsablePhone = responsable_phone,
    ville = ville,
    email = email,
    otherLinks = other_links
)

fun Fournisseur.toDb(): DbFournisseur = DbFournisseur(
    id = id,
    full_name = fullName,
    societe = societe,
    phone = phone,
    responsable_phone = responsablePhone,
    ville = ville,
    email = email,
    other_links = otherLinks
)

fun DbConvention.toDomain(items: List<ConventionItem> = emptyList()): Convention = Convention(
    id = id,
    fournisseurId = fournisseur_id,
    ordNumber = ord_number,
    date = date,
    globalUnitPrice = global_unit_price,
    totalMontant = total_montant,
    tvaRate = tva_rate,
    items = items
)

fun DbConventionItem.toDomain(): ConventionItem = ConventionItem(
    id = id,
    conventionId = convention_id,
    designation = designation,
    unite = unite,
    quantity = quantity,
    minQuantity = min_quantity
)

fun DbService.toDomain(chefName: String? = null): Service = Service(
    id = id,
    name = name,
    localNum = local_num,
    chefId = chef_id,
    chefName = chefName
)

fun DbLocal.toDomain(): Local = Local(
    id = id,
    serviceId = service_id,
    name = name
)

fun DbStockArticle.toDomain(): StockArticle = StockArticle(
    id = id,
    codeArticle = code_article,
    additionalReferenceId = additional_reference_id,
    designation = designation,
    uniteQuantite = unite_quantite,
    family = family,
    sousFamily = sous_family,
    quantity = quantity,
    unitPrice = unit_price,
    expirationDate = expiration_date,
    isArchived = is_archived != 0L,
    isObsolete = is_obsolete != 0L,
    date = date
)

fun DbArticleAuditLog.toDomain(articleDesignation: String = "", articleCode: String = ""): ArticleAuditLog = ArticleAuditLog(
    id = id,
    articleId = article_id,
    fieldName = field_name,
    oldValue = old_value,
    newValue = new_value,
    reason = reason,
    userName = user_name,
    editDate = edit_date,
    articleDesignation = articleDesignation,
    articleCode = articleCode
)

fun DbPerson.toDomain(serviceName: String = "", isChef: Boolean = false): Person = Person(
    id = id,
    serviceId = service_id,
    fullName = full_name,
    birthDate = birth_date,
    notes = notes,
    serviceName = serviceName,
    isChef = isChef
)

fun DbEquipmentAssignment.toDomain(
    personName: String = "",
    articleDesignation: String = "",
    articleCode: String = "",
    unite: String = ""
): EquipmentAssignment = EquipmentAssignment(
    id = id,
    personId = person_id,
    articleId = article_id,
    quantity = quantity,
    assignedDate = assigned_date,
    returnDate = return_date,
    status = status,
    notes = notes,
    personName = personName,
    articleDesignation = articleDesignation,
    articleCode = articleCode,
    unite = unite
)

fun DbDynamicConfigItem.toDomain(): DynamicConfigItem = DynamicConfigItem(
    id = id,
    category = category,
    code = code,
    label = label,
    parentCategory = parent_category,
    parentCode = parent_code,
    isActive = is_active != 0L
)

fun DbUserProfile.toDomain(): UserProfile = UserProfile(
    id = id,
    fullName = full_name,
    phone = phone,
    email = email,
    role = role,
    themeMode = theme_mode,
    locale = locale
)

fun org.abdat.inventory.database.SelectAllPersons.toDomain(): Person = Person(
    id = id,
    serviceId = service_id,
    fullName = full_name,
    birthDate = birth_date,
    notes = notes,
    serviceName = service_name,
    isChef = is_chef != 0L
)

fun org.abdat.inventory.database.SelectPersonsByServiceId.toDomain(): Person = Person(
    id = id,
    serviceId = service_id,
    fullName = full_name,
    birthDate = birth_date,
    notes = notes,
    serviceName = service_name,
    isChef = is_chef != 0L
)

fun org.abdat.inventory.database.SelectAllAssignments.toDomain(): EquipmentAssignment = EquipmentAssignment(
    id = id,
    personId = person_id,
    articleId = article_id,
    quantity = quantity,
    assignedDate = assigned_date,
    returnDate = return_date,
    status = status,
    notes = notes,
    personName = person_name,
    articleDesignation = article_designation,
    articleCode = article_code,
    unite = unite
)

fun org.abdat.inventory.database.SelectAssignmentsByPersonId.toDomain(): EquipmentAssignment = EquipmentAssignment(
    id = id,
    personId = person_id,
    articleId = article_id,
    quantity = quantity,
    assignedDate = assigned_date,
    returnDate = return_date,
    status = status,
    notes = notes,
    personName = person_name,
    articleDesignation = article_designation,
    articleCode = article_code,
    unite = unite
)

fun org.abdat.inventory.database.SelectPersonById.toDomain(): Person = Person(
    id = id,
    serviceId = service_id,
    fullName = full_name,
    birthDate = birth_date,
    notes = notes,
    serviceName = service_name,
    isChef = is_chef != 0L
)

fun org.abdat.inventory.database.SelectChefByServiceId.toDomain(): Person = Person(
    id = id,
    serviceId = service_id,
    fullName = full_name,
    birthDate = birth_date,
    notes = notes,
    serviceName = service_name,
    isChef = true
)

fun org.abdat.inventory.database.SelectAllServicesWithChef.toDomain(): Service = Service(
    id = id,
    name = name,
    localNum = local_num,
    chefId = chef_id,
    chefName = chef_name
)

fun org.abdat.inventory.database.SelectServiceByIdWithChef.toDomain(): Service = Service(
    id = id,
    name = name,
    localNum = local_num,
    chefId = chef_id,
    chefName = chef_name
)
