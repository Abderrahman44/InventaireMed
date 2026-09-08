package org.abdat.inventory.di

import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.database.JvmDatabaseDriverFactory
import org.koin.core.KoinApplication
import org.koin.dsl.module

val jvmPlatformModule = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
}

fun initDesktopKoin(): KoinApplication = initKoin {
    modules(jvmPlatformModule)
}
