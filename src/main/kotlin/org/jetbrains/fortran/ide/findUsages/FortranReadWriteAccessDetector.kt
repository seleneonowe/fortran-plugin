package org.jetbrains.fortran.ide.findUsages

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import io.ktor.util.reflect.*
import org.jetbrains.fortran.lang.psi.*
import kotlin.reflect.KClass

class FortranReadWriteAccessDetector : ReadWriteAccessDetector() {
    override fun isReadWriteAccessible(element: PsiElement): Boolean {
        if (element is FortranProcedureDesignator) {
            return false
        }
        return true
    }

    override fun isDeclarationWriteAccess(element: PsiElement): Boolean {
        return false
    }

    private fun getDeclarationAccess(element: PsiElement): Access {
        if (isDeclarationWriteAccess(element)) {
            return Access.Write
        }
        return Access.Read
    }

    private fun getCallAccess(element: PsiElement): Access {
        return Access.Write
    }

    private fun getDataPathAccess(element: PsiElement): Access {
        if (element.parent.parent is FortranAssignmentStmt) {
            return Access.Write
        }
        if (element.parent.parent.parent.parent.parent is FortranCallStmt) {
            return Access.Write
        }
        return Access.Read
    }

    override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access =
        getExpressionAccess(referencedElement)

    override fun getExpressionAccess(expression: PsiElement): Access {
        // first we check if we're in a type declaration statement
        if (isNestedWithin(expression, FortranTypeDeclarationStmt::class)) {
            return getDeclarationAccess(expression)
        }

        if (isExpressionArrayIndex(expression)) {
            return Access.Read
        }

        // check expression is on the left or right side of an assignment statement
        if (expression.parent is FortranAssignmentStmt) {
            if (expression === expression.parent.firstChild) return Access.Write
            return Access.Read
        }

        //
        val access = when (expression) {
            is FortranTypeDeclarationStmt -> getDeclarationAccess(expression)
            is FortranEntityDecl -> getDeclarationAccess(expression)
            is FortranAssignmentStmt -> Access.Write
            is FortranExplicitShapeSpec -> Access.Read
            is FortranCallStmt -> getCallAccess(expression)
            else -> getDefaultExpressionAccess(expression)
        }
        return access
    }

    private fun isExpressionArrayIndex(expression: PsiElement): Boolean {
        return isExpressionArrayIndexInSubroutineCall(expression)
                || isExpressionArrayIndexInLeftOfAssignment(expression)
    }

    private fun getDefaultExpressionAccess(expression: PsiElement?): Access {
        val context = expression?.context ?: return Access.Read
        return getExpressionAccess(context)
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