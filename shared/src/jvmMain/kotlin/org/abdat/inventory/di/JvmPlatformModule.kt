package org.abdat.inventory.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.database.JvmDatabaseDriverFactory
import org.koin.core.KoinApplication
import org.koin.dsl.module

val jvmPlatformModule = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }

    // Query observation runs blocking JDBC work, so it belongs on the IO pool. Bound here rather
    // than referenced from commonMain because Dispatchers.IO is not declared in the common source
    // set — it would compile while `jvm()` is the only target and break on the next one added.
    single<CoroutineDispatcher> { Dispatchers.IO }
}

fun initDesktopKoin(): KoinApplication = initKoin {
    modules(jvmPlatformModule)
}
