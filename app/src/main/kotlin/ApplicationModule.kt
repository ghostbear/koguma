package me.ghostbear.koguma

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.ghostbear.koguma.data.dataModule
import me.ghostbear.koguma.di.KordContext
import me.ghostbear.koguma.domain.domainModule
import me.ghostbear.koguma.presentation.presentationModule
import me.ghostbear.koguma.session.sessionModule
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module

val applicationModule = module {
    includes(
        domainModule,
        dataModule,
        sessionModule,
        presentationModule,
    )

    single<CoroutineScope> {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                scope.close()
            }
        })
        scope
    }

    single { KordContext() }
    single { ConfigFactory.load() }
    singleOf(::ApplicationPreference)
}