package org.jetbrains.fortran.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.elementType
import io.ktor.util.reflect.*
import org.jetbrains.fortran.lang.psi.*
import kotlin.reflect.KClass

class FortranDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(
        element: PsiElement?,
        originalElement: PsiElement?
    ): String {
        if (element == null || originalElement == null) return ""
//        if (!((originalElement as LeafPsiElement).elementType as FortranTokenType) {
//            return "I am not an entity declaration, maybe I shouldn't have a docstring"
//        }
        if (isSubroutine(element)) {
            // todo: get arguments of subroutine
            //  show an argument list, showing data type, shape, and usage within the subroutine (read/write)
            //  possibly also: link to #include statements where read/write couldn't be determined
            var argString = ""
            for (arg in element.childrenOfType<FortranDataPath>()) {
                argString += "\n" + arg.text
            }
            return "hi im a subroutine, my arguments are $argString"
        }
        else if (isVariable(element)) {
            var shape_str = "1"
            if (element.childrenOfType<FortranExplicitShapeSpec>().isNotEmpty()) {
                val shapeSpecs = element.childrenOfType<FortranExplicitShapeSpec>()
                shape_str = "("
                for (shapeSpec in shapeSpecs) {
                    shape_str += shapeSpec.text + ", "
                }
                shape_str = shape_str.dropLast(2)
                shape_str += ")"
            }
            val myType = element.context!!.firstChild.text.uppercase()
            return "hi im a variable of shape: $shape_str and type $myType"
        }
        return originalElement.elementType.toString()
    }

    private fun isSubroutine(element: PsiElement): Boolean {
        if (
            isNestedWithin(element, FortranSubroutineStmt::class, 1)
            ) {
            return true
        }
        return false
    }

    private fun isVariable(element: PsiElement): Boolean {
        return isNestedWithin(element, FortranTypeDeclarationStmt::class, 1)
    }

    private fun isExpressionArrayIndex(expression: PsiElement): Boolean {
        return isExpressionArrayIndexInSubroutineCall(expression)
                || isExpressionArrayIndexInLeftOfAssignment(expression)
    }
    private fun isExpressionArrayIndexInSubroutineCall(expression: PsiElement): Boolean {
        return false
        // TODO: write this
//        if (isNestedWithin(expression, FortranSectionSubscript::class)) return true
//        return isNestedWithin(expression, FortranCallStmt::class)
    }
    private fun isNestedWithin(expression: PsiElement, klass: KClass<out Any>, depth: Int? = null): Boolean {
        var context: PsiElement? = expression.context ?: return false

        var i = 0
        while (context != null && depth?.let { i < it } != false) {
            if (context.instanceOf(klass)) {
                return true
            }
            context = context.context
            i++
        }
        return false
    }

    private fun isExpressionArrayIndexInLeftOfAssignment(expression: PsiElement): Boolean {
        // check we aren't too close to an assignment statement
        if (expression.context is FortranAssignmentStmt) return false
        if (expression.context?.context is FortranAssignmentStmt) return false

        var context: PsiElement? = expression.context
        while (context != null) {
            if (context is FortranDesignator
                && context.parent is FortranAssignmentStmt
                && context === context.parent.firstChild
            ) {
                return true
            }
            context = context.context
        }
        return false
    }
}