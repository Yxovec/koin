package org.koin

import org.koin.core.bean.BeanDefinition
import org.koin.core.bean.BeanRegistry
import org.koin.core.instance.InstanceFactory
import org.koin.core.property.PropertyRegistry
import org.koin.core.scope.Scope
import org.koin.error.DependencyResolutionException
import java.util.*
import kotlin.reflect.KClass

/**
 * Koin Application Context
 * Context from where you can get beans defines in modules
 *
 * @author Arnaud GIULIANI
 */
class KoinContext(val beanRegistry: BeanRegistry, val propertyResolver: PropertyRegistry, val instanceFactory: InstanceFactory) {

    /**
     * resolution stack
     */
    val resolutionStack = Stack<KClass<*>>()

    /**
     * Retrieve a bean instance
     */
    inline fun <reified T> get(name: String = ""): T = if (name.isEmpty()) resolveByClass() else resolveByName(name)

    /**
     * Resolve a dependency for its bean definition
     */
    inline fun <reified T> resolveByName(name: String) = resolveInstance<T> { beanRegistry.searchByName(name) }

    /**
     * Resolve a dependency for its bean definition
     */
    inline fun <reified T> resolveByClass(): T = resolveInstance { beanRegistry.searchAll(T::class) }

    /**
     * Resolve a dependency for its bean definition
     */
    inline fun <reified T> resolveInstance(resolver: () -> BeanDefinition<*>): T {
        val clazz = T::class
        if (resolutionStack.contains(clazz)) {
            throw DependencyResolutionException("Cyclic dependency for $clazz")
        }
        resolutionStack.add(clazz)

        val beanDefinition: BeanDefinition<*> = resolver()

        val instance = instanceFactory.retrieveInstance<T>(beanDefinition)
        val head = resolutionStack.pop()
        if (head != clazz) {
            resolutionStack.clear()
            throw IllegalStateException("Calling HEAD was $head but must be $clazz")
        }
        return instance
    }

    /**
     * TODO
     */
    fun dryRun() {
        beanRegistry.definitions.keys.forEach { def ->
            instanceFactory.retrieveInstance<Any>(def)
        }
    }

    /**
     * TODO
     */
    fun release(name: String) {
        val definitions: List<BeanDefinition<*>> = beanRegistry.definitionsFromScope(name)
        instanceFactory.dropAllInstances(definitions)
    }

    /**
     * Retrieve a property by its key
     * can return null
     */
    inline fun <reified T> getProperty(key: String): T? = propertyResolver.getProperty(key)

    /**
     * Set a property
     */
    fun setProperty(key: String, value: Any?) = propertyResolver.setProperty(key, value)
}