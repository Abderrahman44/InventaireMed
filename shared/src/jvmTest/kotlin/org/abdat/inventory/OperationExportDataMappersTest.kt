package org.abdat.inventory

import org.abdat.inventory.domain.model.BonDeCommande
import org.abdat.inventory.domain.model.DechargeItem
import org.abdat.inventory.domain.model.PapierDecharge
import org.abdat.inventory.ui.consumption.ConsumptionItem
import org.abdat.inventory.ui.deliveries.DeliveryDisplayItem
import org.abdat.inventory.ui.orders.OrderDisplayItem
import org.abdat.inventory.ui.sorties.SortieDisplayItem
import org.abdat.inventory.util.ConsumptionExportDataMapper
import org.abdat.inventory.util.DechargesExportDataMapper
import org.abdat.inventory.util.DeliveriesExportDataMapper
import org.abdat.inventory.util.OrdersExportDataMapper
import org.abdat.inventory.util.SortiesExportDataMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationExportDataMappersTest {

    @Test
    fun testOrdersExportDataMapper() {
        val sampleOrders = listOf(
            OrderDisplayItem(
                commande = BonDeCommande(
                    id = 1,
                    referenceBon = "CMD-2026-00101",
                    fournisseurId = 1,
                    date = "2026-09-01"
                ),
                supplierName = "SARL PharmaBio",
                itemsCount = 8,
                totalAmountDzd = 450000.0,
                statusLabel = "LIVRÉE",
                progressPercent = 100
            ),
            OrderDisplayItem(
                commande = BonDeCommande(
                    id = 2,
                    referenceBon = "CMD-2026-00102",
                    fournisseurId = 2,
                    date = "2026-09-05"
                ),
                supplierName = "MediTech Algerie",
                itemsCount = 3,
                totalAmountDzd = 125000.0,
                statusLabel = "EN ATTENTE",
                progressPercent = 0
            )
        )

        val dataSet = OrdersExportDataMapper.toExportDataSet(sampleOrders, filterDescription = "Statut : Tous")

        assertEquals("Registre des Bons de Commande", dataSet.title)
        assertTrue(dataSet.subtitle.contains("Statut : Tous"))
        assertEquals(6, dataSet.headers.size)
        assertEquals(2, dataSet.rows.size)

        // Verify column count matches header count for each row
        dataSet.rows.forEach { row ->
            assertEquals(dataSet.headers.size, row.size, "Row length must match headers length")
        }

        // Check metadata
        assertEquals("2", dataSet.metadata["Total Commandes"])
        assertEquals("11", dataSet.metadata["Articles Commandés"])
        assertTrue(dataSet.metadata.containsKey("Montant Total"))
        assertEquals("Statut : Tous", dataSet.metadata["Filtre actif"])

        // First row assertions
        val row1 = dataSet.rows[0]
        assertEquals("CMD-2026-00101", row1[0])
        assertEquals("2026-09-01", row1[1])
        assertEquals("SARL PharmaBio", row1[2])
        assertEquals("LIVRÉE", row1[3])
        assertEquals("8", row1[4])
    }

    @Test
    fun testDeliveriesExportDataMapper() {
        val sampleDeliveries = listOf(
            DeliveryDisplayItem(
                id = 10,
                receptionNumber = "REC-2026-0042",
                paperBlReference = "BL-9988",
                commandeReference = "CMD-2026-00101",
                supplierName = "SARL PharmaBio",
                supplierConvention = "Conv-2026",
                destinationStock = "Pharmacie Centrale",
                serviceName = "Service Pharmacie",
                beneficiaireName = "Interne",
                chefDeServiceName = "Dr. Belkacem",
                dateLivraison = "2026-09-08",
                itemsCount = 5,
                totalAmountDzd = 300000.0,
                statusLabel = "VALIDÉE"
            )
        )

        val dataSet = DeliveriesExportDataMapper.toExportDataSet(sampleDeliveries, filterDescription = "Fournisseur: PharmaBio")

        assertEquals("Registre des Bons de Réception & Livraisons", dataSet.title)
        assertEquals(9, dataSet.headers.size)
        assertEquals(1, dataSet.rows.size)

        dataSet.rows.forEach { row ->
            assertEquals(dataSet.headers.size, row.size)
        }

        val row = dataSet.rows[0]
        assertEquals("REC-2026-0042", row[0])
        assertEquals("2026-09-08", row[1])
        assertEquals("CMD-2026-00101", row[2])
        assertEquals("BL-9988", row[3])
        assertEquals("SARL PharmaBio", row[4])
        assertEquals("Pharmacie Centrale", row[5])
        assertEquals("5", row[6])
        assertEquals("VALIDÉE", row[8])

        assertEquals("1", dataSet.metadata["Total Bons de Réception"])
        assertEquals("5", dataSet.metadata["Articles Réceptionnés"])
    }

    @Test
    fun testSortiesExportDataMapper() {
        val sampleSorties = listOf(
            SortieDisplayItem(
                id = 20,
                referenceBon = "BS-2026-0015",
                paperStubReference = "TALON-15",
                serviceName = "Service Pédiatrie",
                localName = "Salle de Soins 2",
                recipient = "Mme. Mansouri",
                demandeur = "Dr. Amrani",
                recepteurInterne = "Mme. Mansouri",
                date = "2026-09-12 10:30",
                itemsCount = 4,
                totalAmountDzd = 12000.0,
                statusLabel = "VALIDÉ"
            )
        )

        val dataSet = SortiesExportDataMapper.toExportDataSet(sampleSorties, filterDescription = "Service: Pédiatrie")

        assertEquals("Registre des Bons de Sortie Matériel", dataSet.title)
        assertEquals(9, dataSet.headers.size)
        assertEquals(1, dataSet.rows.size)

        dataSet.rows.forEach { row ->
            assertEquals(dataSet.headers.size, row.size)
        }

        val row = dataSet.rows[0]
        assertEquals("BS-2026-0015", row[0])
        assertEquals("2026-09-12 10:30", row[1])
        assertEquals("Service Pédiatrie", row[2])
        assertEquals("Salle de Soins 2", row[3])
        assertEquals("Dr. Amrani", row[4])
        assertEquals("Mme. Mansouri", row[5])
        assertEquals("4", row[6])
        assertEquals("VALIDÉ", row[8])

        assertEquals("1", dataSet.metadata["Total Bons de Sortie"])
        assertEquals("4", dataSet.metadata["Articles Déstockés"])
    }

    @Test
    fun testDechargesExportDataMapper() {
        val sampleDecharges = listOf(
            PapierDecharge(
                id = 5,
                referenceBon = "DEC-2026-0005",
                organizationName = "Croissant Rouge Algérien",
                donorName = "Section Régionale",
                date = "2026-09-15",
                notes = "Don humanitaire de consommables médicaux",
                items = listOf(
                    DechargeItem(id = 1, dechargeId = 5, codeArticle = "CSM-01", designation = "Compresses stériles", quantity = 500.0, unitPrice = 0.0),
                    DechargeItem(id = 2, dechargeId = 5, codeArticle = "CSM-02", designation = "Bandes de gaze", quantity = 250.0, unitPrice = 0.0)
                )
            )
        )

        val dataSet = DechargesExportDataMapper.toExportDataSet(sampleDecharges)

        assertEquals("Registre des Décharges & Donations", dataSet.title)
        assertEquals(6, dataSet.headers.size)
        assertEquals(1, dataSet.rows.size)

        dataSet.rows.forEach { row ->
            assertEquals(dataSet.headers.size, row.size)
        }

        val row = dataSet.rows[0]
        assertEquals("DEC-2026-0005", row[0])
        assertEquals("2026-09-15", row[1])
        assertEquals("Croissant Rouge Algérien", row[2])
        assertEquals("Section Régionale", row[3])
        assertEquals("2", row[4])
        assertEquals("Don humanitaire de consommables médicaux", row[5])

        assertEquals("1", dataSet.metadata["Total Décharges / Dons"])
        assertEquals("2", dataSet.metadata["Lignes d'articles"])
    }

    @Test
    fun testConsumptionExportDataMapper() {
        val sampleConsumption = listOf(
            ConsumptionItem(
                id = 1,
                sortieId = 10,
                referenceBon = "BS-2026-0018",
                date = "2026-09-18",
                serviceId = 1,
                serviceName = "Service Urgences",
                localId = 2,
                localName = "Pharmacie Décentralisée",
                codeArticle = "MED-009",
                designation = "Morphine 10mg Amp",
                family = "Injectables",
                sousFamily = "Antalgiques",
                quantity = 50.0,
                unitPrice = 120.0,
                totalValue = 6000.0
            ),
            ConsumptionItem(
                id = 2,
                sortieId = 10,
                referenceBon = "BS-2026-0018",
                date = "2026-09-18",
                serviceId = 1,
                serviceName = "Service Urgences",
                localId = 2,
                localName = "Pharmacie Décentralisée",
                codeArticle = "PAN-001",
                designation = "Gaze hydrophile",
                family = "Pansements",
                sousFamily = "",
                quantity = 25.0,
                unitPrice = 40.0,
                totalValue = 1000.0
            )
        )

        val dataSet = ConsumptionExportDataMapper.toExportDataSet(sampleConsumption, filterDescription = "Service: Urgences")

        assertEquals("Rapport Détaillé de Consommation & Dotations", dataSet.title)
        assertEquals(10, dataSet.headers.size)
        assertEquals(2, dataSet.rows.size)

        dataSet.rows.forEach { row ->
            assertEquals(dataSet.headers.size, row.size)
        }

        val row1 = dataSet.rows[0]
        assertEquals("BS-2026-0018", row1[0])
        assertEquals("2026-09-18", row1[1])
        assertEquals("Service Urgences", row1[2])
        assertEquals("Pharmacie Décentralisée", row1[3])
        assertEquals("MED-009", row1[4])
        assertEquals("Morphine 10mg Amp", row1[5])
        assertEquals("Injectables", row1[6])
        assertEquals("50", row1[7])

        assertEquals("2", dataSet.metadata["Lignes de consommation"])
        assertEquals("75", dataSet.metadata["Quantité totale distribuée"])
        assertTrue(dataSet.metadata.containsKey("Coût Global Consommation"))
    }
}
