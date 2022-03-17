import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.impl.source.codeStyle.IndentHelper


/**
 * Sets indent for new elements.
 * But it seems indent doesn't play a role in our case, any value works fine for our simple case.
 */
class ParserIndentHelper : IndentHelper() {
    override fun getIndent(file: PsiFile, element: ASTNode): Int {
        return getIndent(file, element, false)
    }

    override fun getIndent(file: PsiFile, element: ASTNode, includeNonSpace: Boolean): Int {
        return 4
    }
}
