package psi.parser

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect

class ParserPomModel(
    private val treeAspect: TreeAspect = TreeAspect(),
    private val userData: MutableMap<Key<Any>, Any?> = mutableMapOf()
) : PomModel {

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return userData[key as Any] as T?
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userData[key as Key<Any>] = value
    }

    override fun <T : PomModelAspect?> getModelAspect(p0: Class<T>): T {
        return treeAspect as T
    }

    override fun runTransaction(transaction: PomTransaction) {
        transaction.run()
    }
}