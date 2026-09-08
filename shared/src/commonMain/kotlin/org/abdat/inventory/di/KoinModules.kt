package org.abdat.inventory.di

import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.database.createDatabase
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        val driverFactory = get<DatabaseDriverFactory>()
        createDatabase(driverFactory)
    }
}

val appModule = module {
    includes(databaseModule)
}

fun initKoin(appDeclaration: (KoinApplication.() -> Unit)? = null): KoinApplication =
    startKoin {
        appDeclaration?.invoke(this)
        modules(appModule)
    }
