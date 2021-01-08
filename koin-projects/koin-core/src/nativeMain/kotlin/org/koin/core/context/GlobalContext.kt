/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.core.context

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.error.KoinAppAlreadyStartedException
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.PlatformTools
import org.koin.mp.native.MainThreadValue

/**
 * Global context - current Koin Application available globally
 *
 * Support to help inject automatically instances once KoinApp has been started
 *
 * @author Arnaud Giuliani
 */
object GlobalContext : KoinContext {

    private var safeKoin: MainThreadValue<Koin>? = null

    override fun get(): Koin = safeKoin?.get() ?: error("KoinApplication has not been started")

    override fun getOrNull(): Koin? = safeKoin?.get()

    private fun register(koinApplication: KoinApplication) {
        if (safeKoin?.get() != null) {
            throw KoinAppAlreadyStartedException("A Koin Application has already been started")
        }
        safeKoin = MainThreadValue(koinApplication.koin)
    }

    override fun stopKoin() = PlatformTools.synchronized(this) {
        safeKoin?.get()?.close()
        safeKoin = null
    }


    override fun startKoin(koinApplication: KoinApplication): KoinApplication = PlatformTools.synchronized(this) {
        register(koinApplication)
        koinApplication.createEagerInstances()
        koinApplication
    }

    override fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication = PlatformTools.synchronized(this) {
        val koinApplication = KoinApplication.init()
        register(koinApplication)
        appDeclaration(koinApplication)
        koinApplication.createEagerInstances()
        koinApplication
    }


    override fun loadKoinModules(module: Module) = PlatformTools.synchronized(this) {
        get().loadModules(listOf(module))
    }

    override fun loadKoinModules(modules: List<Module>) = PlatformTools.synchronized(this) {
        get().loadModules(modules)
    }

    override fun unloadKoinModules(module: Module) = PlatformTools.synchronized(this) {
        get().unloadModules(listOf(module))
    }

    override fun unloadKoinModules(modules: List<Module>) = PlatformTools.synchronized(this) {
        get().unloadModules(modules)
    }
}