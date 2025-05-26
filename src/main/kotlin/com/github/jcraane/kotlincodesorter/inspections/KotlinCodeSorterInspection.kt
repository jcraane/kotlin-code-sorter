package com.github.jcraane.kotlincodesorter.inspections

import com.github.jcraane.kotlincodesorter.services.KotlinElementSorter
import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.psi.KtFile

/**
 * Inspection tool that integrates Kotlin code sorting with IntelliJ's code cleanup feature.
 *
 * This class implements CleanupLocalInspectionTool which is a marker interface that indicates
 * to IntelliJ that this inspection should be available during code cleanup. When the user runs
 * "Code Cleanup" or selects this inspection in the cleanup profile, the quick fix will be
 * automatically applied to sort the Kotlin code according to the defined rules.
 *
 * The inspection registers an information-level problem for each Kotlin file, which can be
 * fixed by the SortKotlinCodeQuickFix. During code cleanup, this fix is automatically applied.
 */
class KotlinCodeSorterInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    private val sorter = KotlinElementSorter()

    override fun getDisplayName(): String = "Sort Kotlin Code"

    override fun getGroupDisplayName(): String = "Kotlin"

    override fun getShortName(): String = "KotlinCodeSorter"

    override fun isEnabledByDefault(): Boolean = true

    override fun getStaticDescription(): String = "Sorts Kotlin code according to the specified rules."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file !is KtFile) return

                // Register a problem for the file so that it can be fixed during code cleanup
                holder.registerProblem(
                    file,
                    "Kotlin code can be sorted",
                    ProblemHighlightType.WEAK_WARNING,
                    SortKotlinCodeQuickFix()
                )

                super.visitFile(file)
            }
        }
    }

    /**
     * Quick fix that sorts the Kotlin code.
     */
    private inner class SortKotlinCodeQuickFix : LocalQuickFix {
        override fun getName(): String = "Sort Kotlin Code"

        override fun getFamilyName(): String = "Kotlin"

        @OptIn(KaAllowAnalysisOnEdt::class)
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement as? KtFile ?: return
            println("SKJDKSDJSD")
            ApplicationManager.getApplication().runReadAction {
                allowAnalysisOnEdt {
                    sorter.sortFile(project, file)
                }
            }
        }
    }
}
