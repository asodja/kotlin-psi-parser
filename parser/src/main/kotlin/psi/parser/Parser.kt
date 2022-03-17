package psi.parser

import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import psi.parser.Environment.getBindingContext
import psi.parser.Environment.newEnvironment

class Parser {

    companion object {
        const val PROVIDER_CLASS_NAME: String = "psi.parser.Provider"
    }

    fun rewriteClass(providerClass: String, classToRewrite: String): String {
        val env = newEnvironment()
        val started = System.currentTimeMillis()
        val providerFile = createKtFile(env.project, "Provider.kt", providerClass)
        val ktFile = createKtFile(env.project, "SomeKtFile.kt", classToRewrite)
        val bindingContext = getBindingContext(env, listOf(providerFile, ktFile))

        ktFile.children.forEach { psiElement ->
            when (psiElement) {
                is KtClass -> {}
                is KtNamedFunction -> {
                    parseFunction(psiElement, bindingContext)
                }
                is PsiErrorElementImpl -> {
                    println(psiElement.errorDescription)
                }
                else -> {
                    println(psiElement.text + " " + psiElement::class.java)
                }
            }
        }
        println("Parsing took: ${System.currentTimeMillis() - started}ms")
        return ktFile.text
    }

    private fun parseFunction(function: KtNamedFunction, bindingContext: BindingContext) {
        function.children.forEach { child ->
            when (child) {
                is KtBlockExpression -> parseKtBlockExpression(child, bindingContext)
            }
        }
    }

    private fun parseKtBlockExpression(block: KtBlockExpression, bindingContext: BindingContext) {
        block.children.forEach { child ->
            when (child) {
                is KtBinaryExpression -> {
                    if (isProvider(child.left!!, bindingContext)) {
                        val leftText = child.left!!.text
                        val rightText = child.right!!.text
                        val setExpression = KtPsiFactory(child.project, markGenerated = true).createExpression("$leftText.set(${rightText})")
                        child.replace(setExpression)
                    }
                }
            }
        }
    }

    private fun isProvider(left: KtExpression, bindingContext: BindingContext): Boolean {
        return when(left) {
            is KtNameReferenceExpression -> hasProviderInterface(left, bindingContext)
            else -> false
        }
    }

    private fun hasProviderInterface(expr: KtExpression, bindingContext: BindingContext): Boolean {
        return when (val kotlinType = expr.getType(bindingContext)) {
            null -> false
            else -> {
                val classDescriptor = DescriptorUtils.getClassDescriptorForType(kotlinType)
                return classDescriptor.getSuperInterfaces().any { DescriptorUtils.getFqNameSafe(it).toString() == PROVIDER_CLASS_NAME }
            }
        }
    }

    private fun createKtFile(project: Project, fileName: String, codeString: String) = PsiManager.getInstance(project)
        .findFile(
            LightVirtualFile(fileName, KotlinFileType.INSTANCE, codeString)
        ) as KtFile
}
