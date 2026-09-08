package org.abdat.inventory.data.mapper

import org.abdat.inventory.database.Fournisseur as DbFournisseur
import org.abdat.inventory.database.Convention as DbConvention
import org.abdat.inventory.database.Convention_item as DbConventionItem
import org.abdat.inventory.database.Service as DbService
import org.abdat.inventory.database.Local as DbLocal
import org.abdat.inventory.database.Stock_article as DbStockArticle
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.ConventionItem
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.Local
import org.abdat.inventory.domain.model.StockArticle

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

fun DbService.toDomain(): Service = Service(
    id = id,
    name = name,
    localNum = local_num
)

fun DbLocal.toDomain(): Local = Local(
    id = id,
    serviceId = service_id,
    name = name
)

fun DbStockArticle.toDomain(): StockArticle = StockArticle(
    id = id,
    codeArticle = code_article,
    designation = designation,
    uniteQuantite = unite_quantite,
    family = family,
    sousFamily = sous_family,
    quantity = quantity,
    unitPrice = unit_price,
    expirationDate = expiration_date,
    date = date
)
