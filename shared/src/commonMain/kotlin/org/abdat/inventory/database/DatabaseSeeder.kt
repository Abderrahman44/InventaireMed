package org.abdat.inventory.database

fun seedInitialDataIfEmpty(database: AppDatabase) {
    val existingArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
    if (existingArticles.isNotEmpty()) return

    database.transaction {
        // 1. Seed Articles
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            unite_quantite = "FLACON",
            family = "Médicaments",
            sous_family = "Antalgique",
            quantity = 450.0,
            unit_price = 120.0,
            expiration_date = "2027-12-31",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-002",
            designation = "Amoxicilline + Acide Clavulanique 1g",
            unite_quantite = "BOITE",
            family = "Médicaments",
            sous_family = "Antibiotique",
            quantity = 120.0,
            unit_price = 850.0,
            expiration_date = "2027-06-30",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-003",
            designation = "Insuline Rapide 100 UI/ml",
            unite_quantite = "FLACON",
            family = "Médicaments",
            sous_family = "Hormones",
            quantity = 0.0,
            unit_price = 1450.0,
            expiration_date = "2027-04-15",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MAT-101",
            designation = "Seringues Stériles 5ml 3 pièces",
            unite_quantite = "CARTON",
            family = "Consommables",
            sous_family = "Injection",
            quantity = 35.0,
            unit_price = 3500.0,
            expiration_date = "2029-01-01",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MAT-102",
            designation = "Tubulures de Perfusion avec Régulateur",
            unite_quantite = "PAQUET",
            family = "Consommables",
            sous_family = "Perfusion",
            quantity = 8.0,
            unit_price = 420.0,
            expiration_date = "2028-10-31",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "REA-201",
            designation = "Bandelettes Réactives Glycémie Capillaire",
            unite_quantite = "BOITE",
            family = "Réactifs",
            sous_family = "Diagnostic",
            quantity = 85.0,
            unit_price = 2100.0,
            expiration_date = "2027-08-30",
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "EQU-301",
            designation = "Tensiomètres Électroniques Bras Professionnel",
            unite_quantite = "UNITE",
            family = "Matériel",
            sous_family = "Monitoring",
            quantity = 15.0,
            unit_price = 18500.0,
            expiration_date = null,
            date = "2026-09-08"
        )

        // 2. Seed Suppliers
        database.appDatabaseQueries.insertFournisseur("SARL ALGÉRIE MÉDICAL TECH")
        val idFrn1 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertFournisseur("BIOPHARM DISTRIBUTION SPA")
        val idFrn2 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertFournisseur("GROUPE SAIDAL PHARMA SPA")
        val idFrn3 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        // 3. Seed Conventions with Multiple Articles
        database.appDatabaseQueries.insertConvention(
            fournisseur_id = idFrn1,
            ord_number = 1L,
            date = "2026-01-15",
            global_unit_price = 0.0,
            total_montant = 15000000.0
        )
        val conv1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv1Id,
            designation = "Seringues Stériles 5ml 3 pièces",
            unite = "CARTON",
            quantity = 200.0,
            min_quantity = 20.0
        )
        val conv1Item1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv1Id,
            designation = "Tubulures de Perfusion avec Régulateur",
            unite = "PAQUET",
            quantity = 500.0,
            min_quantity = 50.0
        )
        val conv1Item2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertConvention(
            fournisseur_id = idFrn2,
            ord_number = 2L,
            date = "2026-02-01",
            global_unit_price = 0.0,
            total_montant = 28000000.0
        )
        val conv2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv2Id,
            designation = "Paracétamol 1g Perfusion IV",
            unite = "FLACON",
            quantity = 2000.0,
            min_quantity = 100.0
        )
        val conv2Item1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv2Id,
            designation = "Amoxicilline + Acide Clavulanique 1g",
            unite = "BOITE",
            quantity = 800.0,
            min_quantity = 50.0
        )
        val conv2Item2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        // 4. Seed Commandes with Linked Convention Articles
        database.appDatabaseQueries.insertCommande(
            reference_bon = "BC-2026-0841",
            fournisseur_id = idFrn2,
            date = "2026-08-28",
            is_debt = 0L
        )
        val cmd1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd1Id,
            convention_item_id = conv2Item1Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            type = "CONVENTION",
            family = "Médicaments",
            quantity = 500.0,
            unit_price = 120.0
        )
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd1Id,
            convention_item_id = conv2Item2Id,
            code_article = "MED-002",
            designation = "Amoxicilline + Acide Clavulanique 1g",
            type = "CONVENTION",
            family = "Médicaments",
            quantity = 200.0,
            unit_price = 850.0
        )

        database.appDatabaseQueries.insertCommande(
            reference_bon = "BC-2026-0842",
            fournisseur_id = idFrn1,
            date = "2026-09-02",
            is_debt = 0L
        )
        val cmd2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd2Id,
            convention_item_id = conv1Item1Id,
            code_article = "MAT-101",
            designation = "Seringues Stériles 5ml 3 pièces",
            type = "CONVENTION",
            family = "Consommables",
            quantity = 50.0,
            unit_price = 3500.0
        )

        database.appDatabaseQueries.insertCommande(
            reference_bon = "BC-2026-0843",
            fournisseur_id = idFrn3,
            date = "2026-09-05",
            is_debt = 1L
        )
        val cmd3Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd3Id,
            convention_item_id = null,
            code_article = "MED-003",
            designation = "Insuline Rapide 100 UI/ml",
            type = "URGENCE",
            family = "Médicaments",
            quantity = 50.0,
            unit_price = 1450.0
        )

        // 5. Seed Delivery (BL)
        database.appDatabaseQueries.insertLivraison(
            reference_bon = "BL-2026-0312",
            fournisseur_id = idFrn2,
            date_livraison = "2026-09-01",
            commande_id = cmd1Id
        )
        val bl1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLivraisonItem(
            livraison_id = bl1Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            quantity = 500.0,
            unit_price = 120.0
        )
    }
}
