package org.abdat.inventory.data.mapper

import org.abdat.inventory.database.Bon_de_commande as DbBonDeCommande
import org.abdat.inventory.database.Commande_item as DbCommandeItem
import org.abdat.inventory.database.Bon_de_livraison as DbBonDeLivraison
import org.abdat.inventory.database.Livraison_item as DbLivraisonItem
import org.abdat.inventory.database.Bon_de_sortie as DbBonDeSortie
import org.abdat.inventory.database.Sortie_item as DbSortieItem
import org.abdat.inventory.domain.model.BonDeCommande
import org.abdat.inventory.domain.model.CommandeItem
import org.abdat.inventory.domain.model.BonDeLivraison
import org.abdat.inventory.domain.model.LivraisonItem
import org.abdat.inventory.domain.model.BonDeSortie
import org.abdat.inventory.domain.model.SortieItem

fun DbBonDeCommande.toDomain(items: List<CommandeItem> = emptyList()): BonDeCommande = BonDeCommande(
    id = id,
    referenceBon = reference_bon,
    fournisseurId = fournisseur_id,
    date = date,
    isDebt = is_debt != 0L,
    items = items
)

fun DbCommandeItem.toDomain(): CommandeItem = CommandeItem(
    id = id,
    commandeId = commande_id,
    conventionItemId = convention_item_id,
    codeArticle = code_article,
    designation = designation,
    type = type,
    family = family,
    quantity = quantity,
    unitPrice = unit_price
)

fun DbBonDeLivraison.toDomain(items: List<LivraisonItem> = emptyList()): BonDeLivraison = BonDeLivraison(
    id = id,
    referenceBon = reference_bon,
    fournisseurId = fournisseur_id,
    dateLivraison = date_livraison,
    commandeId = commande_id,
    items = items
)

fun DbLivraisonItem.toDomain(): LivraisonItem = LivraisonItem(
    id = id,
    livraisonId = livraison_id,
    codeArticle = code_article,
    designation = designation,
    quantity = quantity,
    unitPrice = unit_price
)

fun DbBonDeSortie.toDomain(items: List<SortieItem> = emptyList()): BonDeSortie = BonDeSortie(
    id = id,
    referenceBon = reference_bon,
    serviceId = service_id,
    localId = local_id,
    date = date,
    items = items
)

fun DbSortieItem.toDomain(): SortieItem = SortieItem(
    id = id,
    sortieId = sortie_id,
    codeArticle = code_article,
    designation = designation,
    quantity = quantity
)
