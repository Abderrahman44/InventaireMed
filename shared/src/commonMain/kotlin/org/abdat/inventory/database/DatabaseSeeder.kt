package org.abdat.inventory.database

fun seedInitialDataIfEmpty(database: AppDatabase) {
    val existingArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
    if (existingArticles.isNotEmpty()) return

    database.transaction {
        // 1. Seed Articles
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-001",
            additional_reference_id = "6130001001234",
            designation = "Paracétamol 1g Perfusion IV",
            unite_quantite = "FLACON",
            family = "Médicaments",
            sous_family = "Antalgique",
            quantity = 450.0,
            unit_price = 120.0,
            expiration_date = "2027-12-31",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val art1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-002",
            additional_reference_id = "6130001005678",
            designation = "Amoxicilline + Acide Clavulanique 1g",
            unite_quantite = "BOITE",
            family = "Médicaments",
            sous_family = "Antibiotique",
            quantity = 120.0,
            unit_price = 850.0,
            expiration_date = "2027-06-30",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MED-003",
            additional_reference_id = "6130001009101",
            designation = "Insuline Rapide 100 UI/ml",
            unite_quantite = "FLACON",
            family = "Médicaments",
            sous_family = "Hormones",
            quantity = 0.0,
            unit_price = 1450.0,
            expiration_date = "2027-04-15",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MAT-101",
            additional_reference_id = "6130002001122",
            designation = "Seringues Stériles 5ml 3 pièces",
            unite_quantite = "CARTON",
            family = "Consommables",
            sous_family = "Injection",
            quantity = 35.0,
            unit_price = 3500.0,
            expiration_date = "2029-01-01",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "MAT-102",
            additional_reference_id = "6130002003344",
            designation = "Tubulures de Perfusion avec Régulateur",
            unite_quantite = "PAQUET",
            family = "Consommables",
            sous_family = "Perfusion",
            quantity = 8.0,
            unit_price = 420.0,
            expiration_date = "2028-10-31",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "REA-201",
            additional_reference_id = "6130003005566",
            designation = "Bandelettes Réactives Glycémie Capillaire",
            unite_quantite = "BOITE",
            family = "Réactifs",
            sous_family = "Diagnostic",
            quantity = 85.0,
            unit_price = 2100.0,
            expiration_date = "2027-08-30",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "EQU-301",
            additional_reference_id = "6130004007788",
            designation = "Tensiomètres Électroniques Bras Professionnel",
            unite_quantite = "PIECE",
            family = "Matériel",
            sous_family = "Monitoring",
            quantity = 15.0,
            unit_price = 18500.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val equId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        // Quincaillerie générale
        database.appDatabaseQueries.insertStockArticle(
            code_article = "QNC-001",
            additional_reference_id = "6130005001111",
            designation = "Jeu de Tournevis Isolés 1000V Pro",
            unite_quantite = "JEU",
            family = "Quincaillerie générale",
            sous_family = "Outillage",
            quantity = 12.0,
            unit_price = 4200.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "QNC-002",
            additional_reference_id = "6130005002222",
            designation = "Câble Électrique Cuivre 2.5mm² (Rouleau 100m)",
            unite_quantite = "ROULEAU",
            family = "Quincaillerie générale",
            sous_family = "Électricité",
            quantity = 8.0,
            unit_price = 8500.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "QNC-003",
            additional_reference_id = "6130005003333",
            designation = "Vannes d'Arrêt à Bille Laiton 1/2\"",
            unite_quantite = "PIECE",
            family = "Quincaillerie générale",
            sous_family = "Plomberie",
            quantity = 30.0,
            unit_price = 950.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )

        // Accessoire informatique (PC & périphériques)
        database.appDatabaseQueries.insertStockArticle(
            code_article = "INF-001",
            additional_reference_id = "6130006001111",
            designation = "PC de Bureau HP ProDesk Core i5 16Go SSD 512Go",
            unite_quantite = "UNITE",
            family = "Accessoire informatique",
            sous_family = "Ordinateurs",
            quantity = 25.0,
            unit_price = 89000.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "INF-002",
            additional_reference_id = "6130006002222",
            designation = "Câble Réseau RJ45 UTP Cat6 5m",
            unite_quantite = "PIECE",
            family = "Accessoire informatique",
            sous_family = "Réseau",
            quantity = 60.0,
            unit_price = 650.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "INF-003",
            additional_reference_id = "6130006003333",
            designation = "Souris Optique Filaire USB Pro",
            unite_quantite = "PIECE",
            family = "Accessoire informatique",
            sous_family = "Périphériques",
            quantity = 45.0,
            unit_price = 1200.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "INF-004",
            additional_reference_id = "6130006004444",
            designation = "Cartouche Toner Noir HP LaserJet Pro",
            unite_quantite = "BOITE",
            family = "Accessoire informatique",
            sous_family = "Consommables informatiques",
            quantity = 18.0,
            unit_price = 6800.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )

        // Accessoire bureautique
        database.appDatabaseQueries.insertStockArticle(
            code_article = "BUR-001",
            additional_reference_id = "6130007001111",
            designation = "Rame Papier Blanc A4 80g (Carton de 5)",
            unite_quantite = "CARTON",
            family = "Accessoire bureautique",
            sous_family = "Papeterie",
            quantity = 50.0,
            unit_price = 3800.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "BUR-002",
            additional_reference_id = "6130007002222",
            designation = "Boîte de 50 Stylos à Bille Bleu 1.0mm",
            unite_quantite = "BOITE",
            family = "Accessoire bureautique",
            sous_family = "Fournitures de bureau",
            quantity = 35.0,
            unit_price = 1100.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "BUR-003",
            additional_reference_id = "6130007003333",
            designation = "Classeurs à Levier Dos 75mm Grand Format",
            unite_quantite = "PAQUET",
            family = "Accessoire bureautique",
            sous_family = "Classement",
            quantity = 40.0,
            unit_price = 2200.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )

        // Imprimés divers
        database.appDatabaseQueries.insertStockArticle(
            code_article = "IMP-001",
            additional_reference_id = "6130008001111",
            designation = "Carnets de Bons de Sortie Interne Triplicata (50f)",
            unite_quantite = "CARNET",
            family = "Imprimés divers",
            sous_family = "Formulaires administratifs",
            quantity = 80.0,
            unit_price = 450.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "IMP-002",
            additional_reference_id = "6130008002222",
            designation = "Registre d'Admission des Malades Format Grand Livre",
            unite_quantite = "PIECE",
            family = "Imprimés divers",
            sous_family = "Registres",
            quantity = 15.0,
            unit_price = 3200.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        database.appDatabaseQueries.insertStockArticle(
            code_article = "IMP-003",
            additional_reference_id = "6130008003333",
            designation = "Rouleaux d'Étiquettes d'Identification Patients",
            unite_quantite = "ROULEAU",
            family = "Imprimés divers",
            sous_family = "Étiquettes",
            quantity = 40.0,
            unit_price = 1800.0,
            expiration_date = null,
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )


        // 2. Seed Suppliers
        database.appDatabaseQueries.insertFournisseurFull(
            full_name = "SARL ALGÉRIE MÉDICAL TECH",
            societe = "Algérie Médical Technologies SARL",
            phone = "+213 23 45 67 89",
            responsable_phone = "+213 550 12 34 56",
            ville = "Alger (Hydra)",
            email = "contact@algeriemedtech.dz",
            other_links = "wa.me/213550123456 | t.me/AlgerieMedTech"
        )
        val idFrn1 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertFournisseurFull(
            full_name = "BIOPHARM DISTRIBUTION SPA",
            societe = "Biopharm Distribution Logistique SPA",
            phone = "+213 21 78 90 12",
            responsable_phone = "+213 661 45 67 89",
            ville = "Oued Smar, Alger",
            email = "commandes@biopharm-dz.com",
            other_links = "wa.me/213661456789 | t.me/BiopharmSupply"
        )
        val idFrn2 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertFournisseurFull(
            full_name = "GROUPE SAIDAL PHARMA SPA",
            societe = "Groupe Saidal Production & Distribution SPA",
            phone = "+213 21 64 22 00",
            responsable_phone = "+213 770 98 76 54",
            ville = "Constantine / Alger",
            email = "direction.commerciale@saidal.dz",
            other_links = "wa.me/213770987654 | t.me/SaidalPharma"
        )
        val idFrn3 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertFournisseurFull(
            full_name = "SARL INFOBUREAU & QUINCAILLERIE NATIONALE",
            societe = "Infobureau & Quincaillerie Générale SARL",
            phone = "+213 21 24 00 11",
            responsable_phone = "+213 555 88 99 00",
            ville = "Bab Ezzouar, Alger",
            email = "contact@infobureau-dz.com",
            other_links = "wa.me/213555889900 | t.me/InfoBureauDz"
        )
        val idFrn4 = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        // 3. Seed Conventions with Multiple Articles
        database.appDatabaseQueries.insertConvention(
            fournisseur_id = idFrn4,
            ord_number = 3L,
            date = "2026-03-01",
            global_unit_price = 0.0,
            total_montant = 12000000.0,
            tva_rate = 19.0
        )
        val conv3Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv3Id,
            designation = "PC de Bureau HP ProDesk Core i5 16Go SSD 512Go",
            unite = "UNITE",
            quantity = 30.0,
            min_quantity = 2.0
        )
        val conv3Item1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertConventionItem(
            convention_id = conv3Id,
            designation = "Rame Papier Blanc A4 80g (Carton de 5)",
            unite = "CARTON",
            quantity = 150.0,
            min_quantity = 15.0
        )
        val conv3Item2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertConvention(
            fournisseur_id = idFrn1,
            ord_number = 1L,
            date = "2026-01-15",
            global_unit_price = 0.0,
            total_montant = 15000000.0,
            tva_rate = 19.0
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
            total_montant = 28000000.0,
            tva_rate = 9.0
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

        database.appDatabaseQueries.insertCommande(
            reference_bon = "BC-2026-0844",
            fournisseur_id = idFrn4,
            date = "2026-09-02",
            is_debt = 0L
        )
        val cmd4Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd4Id,
            convention_item_id = conv3Item1Id,
            code_article = "INF-001",
            designation = "PC de Bureau HP ProDesk Core i5 16Go SSD 512Go",
            type = "CONVENTION",
            family = "Accessoire informatique",
            quantity = 10.0,
            unit_price = 89000.0
        )
        database.appDatabaseQueries.insertCommandeItem(
            commande_id = cmd4Id,
            convention_item_id = conv3Item2Id,
            code_article = "BUR-001",
            designation = "Rame Papier Blanc A4 80g (Carton de 5)",
            type = "CONVENTION",
            family = "Accessoire bureautique",
            quantity = 40.0,
            unit_price = 3800.0
        )

        // 5. Seed Services & Locals
        database.appDatabaseQueries.insertService(name = "Urgences Médico-Chirurgicales (UMC)", local_num = "LOC-UMC-01")
        val service1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service1Id, name = "Box Déchoquage & Réanimation")
        val local1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service1Id, name = "Pharmacie Relais UMC")
        database.appDatabaseQueries.insertLocal(service_id = service1Id, name = "Petite Chirurgie")

        database.appDatabaseQueries.insertService(name = "Chirurgie Générale & Viscérale", local_num = "LOC-CHIR-02")
        val service2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service2Id, name = "Salle de Soins Infirmiers")
        val local2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service2Id, name = "Chambre Stérile A")

        database.appDatabaseQueries.insertService(name = "Biomédical & Maintenance Technique", local_num = "LOC-TECH-03")
        val service3Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service3Id, name = "Atelier Électronique Biomédical")
        database.appDatabaseQueries.insertLocal(service_id = service3Id, name = "Magasin Pièces Détachées")

        database.appDatabaseQueries.insertService(name = "Pharmacie Centrale & Distribution", local_num = "LOC-PHAR-04")
        val service4Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLocal(service_id = service4Id, name = "Réserve Générale")
        database.appDatabaseQueries.insertLocal(service_id = service4Id, name = "Chambre Froide 2-8°C")

        // 6. Seed Personnel
        database.appDatabaseQueries.insertPerson(
            service_id = service1Id,
            full_name = "Dr. Karim Bensalem",
            birth_date = "1982-04-12",
            notes = "Chirurgien Chef de Clinique"
        )
        val person1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertPerson(
            service_id = service1Id,
            full_name = "Yasmine Khelifi",
            birth_date = "1991-09-24",
            notes = "Infirmière Anesthésiste Major"
        )
        val person2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertPerson(
            service_id = service3Id,
            full_name = "Mourad Hadj",
            birth_date = "1988-11-03",
            notes = "Ingénieur Biomédical"
        )
        val person3Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertPerson(
            service_id = service4Id,
            full_name = "Dr. Amina Mansouri",
            birth_date = "1978-06-15",
            notes = "Chef de Service Pharmacie Centrale"
        )
        val person4Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        database.appDatabaseQueries.insertPerson(
            service_id = service2Id,
            full_name = "Dr. Selim Rahmani",
            birth_date = "1980-02-28",
            notes = "Chef de Service Chirurgie"
        )
        val person5Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

        // Assign Chefs de Service to departments
        database.appDatabaseQueries.updateServiceChef(chef_id = person1Id, id = service1Id)
        database.appDatabaseQueries.updateServiceChef(chef_id = person5Id, id = service2Id)
        database.appDatabaseQueries.updateServiceChef(chef_id = person3Id, id = service3Id)
        database.appDatabaseQueries.updateServiceChef(chef_id = person4Id, id = service4Id)

        // 7. Seed Deliveries (BL)
        database.appDatabaseQueries.insertLivraison(
            reference_bon = "BL-2026-0310",
            fournisseur_id = idFrn2,
            date_livraison = "2026-08-30",
            commande_id = cmd1Id,
            service_id = service4Id,
            beneficiaire_id = person4Id
        )
        val bl0Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLivraisonItem(
            livraison_id = bl0Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            quantity = 300.0,
            unit_price = 120.0
        )
        database.appDatabaseQueries.insertLivraisonItem(
            livraison_id = bl0Id,
            code_article = "MED-002",
            designation = "Amoxicilline + Acide Clavulanique 1g",
            quantity = 100.0,
            unit_price = 850.0
        )

        database.appDatabaseQueries.insertLivraison(
            reference_bon = "BL-2026-0312",
            fournisseur_id = idFrn2,
            date_livraison = "2026-09-01",
            commande_id = cmd1Id,
            service_id = service1Id,
            beneficiaire_id = person2Id
        )
        val bl1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLivraisonItem(
            livraison_id = bl1Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            quantity = 200.0,
            unit_price = 120.0
        )

        database.appDatabaseQueries.insertLivraison(
            reference_bon = "BL-2026-0315",
            fournisseur_id = idFrn1,
            date_livraison = "2026-09-03",
            commande_id = cmd2Id,
            service_id = service3Id,
            beneficiaire_id = person3Id
        )
        val bl2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertLivraisonItem(
            livraison_id = bl2Id,
            code_article = "MAT-101",
            designation = "Seringues Stériles 5ml 3 pièces",
            quantity = 50.0,
            unit_price = 3500.0
        )

        // 7. Seed Initial Bons de Sortie
        database.appDatabaseQueries.insertSortie(
            reference_bon = "BS-2026-001",
            service_id = service1Id,
            local_id = local1Id,
            date = "2026-09-04 09:15"
        )
        val bs1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertSortieItem(
            sortie_id = bs1Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            quantity = 25.0
        )
        database.appDatabaseQueries.insertSortieItem(
            sortie_id = bs1Id,
            code_article = "MAT-101",
            designation = "Seringues Stériles 5ml 3 pièces",
            quantity = 10.0
        )

        database.appDatabaseQueries.insertSortie(
            reference_bon = "BS-2026-002",
            service_id = service2Id,
            local_id = local2Id,
            date = "2026-09-06 14:40"
        )
        val bs2Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertSortieItem(
            sortie_id = bs2Id,
            code_article = "MAT-102",
            designation = "Tubulures de Perfusion avec Régulateur",
            quantity = 5.0
        )

        database.appDatabaseQueries.insertSortie(
            reference_bon = "BS-2026-003",
            service_id = service3Id,
            local_id = null,
            date = "2026-09-07 11:20"
        )
        val bs3Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertSortieItem(
            sortie_id = bs3Id,
            code_article = "INF-001",
            designation = "PC de Bureau HP ProDesk Core i5 16Go SSD 512Go",
            quantity = 2.0
        )
        database.appDatabaseQueries.insertSortieItem(
            sortie_id = bs3Id,
            code_article = "BUR-001",
            designation = "Rame Papier Blanc A4 80g (Carton de 5)",
            quantity = 5.0
        )

        // 8. Seed Dynamic Config (Suggested Units)
        val defaultUnits = listOf("Boîte", "Carton", "Flacon", "Litre", "Pièce", "Rame", "Bidon", "Paquet", "Kit")
        defaultUnits.forEach { unit ->
            database.appDatabaseQueries.insertConfigItem(
                category = "UNIT",
                code = unit.uppercase().replace("Î", "I").replace("È", "E"),
                label = unit,
                parent_category = "",
                parent_code = "",
                is_active = 1L
            )
        }

        // Families and sous-families are reference data, not mock data: see ensureCategoryTaxonomy,
        // which runs on every startup whether or not this seeder does.

        // 9. Seed System Metadata & Defaults
        database.appDatabaseQueries.setMetadata("default_min_quantity", "5.0")
        database.appDatabaseQueries.setMetadata("prefix_commande", "CMD-2026-")
        database.appDatabaseQueries.setMetadata("prefix_livraison", "BL-2026-")
        database.appDatabaseQueries.setMetadata("prefix_sortie", "BS-2026-")
        database.appDatabaseQueries.setMetadata("prefix_decharge", "DCH-2026-")

        // 10. Seed User Profile
        database.appDatabaseQueries.insertOrUpdateUserProfile(
            full_name = "Dr. Amina Mansouri",
            phone = "+213 551 23 45 67",
            email = "a.mansouri@inventairemed.dz",
            role = "Chef Pharmacien / Responsable Stock",
            theme_mode = "LIGHT",
            locale = "FR"
        )

        // 12. Seed Equipment Dotation
        database.appDatabaseQueries.insertEquipmentAssignment(
            person_id = person1Id,
            article_id = equId,
            quantity = 1.0,
            assigned_date = "2026-09-02 11:00",
            return_date = null,
            status = "ACTIVE",
            notes = "Dotation individuelle pour consultations préopératoires"
        )

        // 13. Seed Sample Papier de Décharge (Gift / Donation)
        database.appDatabaseQueries.insertDecharge(
            reference_bon = "DCH-2026-001",
            organization_name = "Ministère de la Santé - Don National d'Urgence",
            donor_name = "Direction Centrale de la Pharmacie",
            date = "2026-09-01 10:00",
            notes = "Subvention étatique d'urgence sanitaire - Articles sans impact budgétaire"
        )
        val dch1Id = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
        database.appDatabaseQueries.insertDechargeItem(
            decharge_id = dch1Id,
            code_article = "MED-001",
            designation = "Paracétamol 1g Perfusion IV",
            quantity = 100.0
        )

        // 14. Seed Sample Audit Log
        database.appDatabaseQueries.insertArticleAuditLog(
            article_id = art1Id,
            field_name = "designation",
            old_value = "Paracetamol 1g",
            new_value = "Paracétamol 1g Perfusion IV",
            reason = "Correction orthographe et spécification voie d'administration IV",
            user_name = "Dr. Amina Mansouri",
            edit_date = "2026-09-07 10:30"
        )
    }
}
