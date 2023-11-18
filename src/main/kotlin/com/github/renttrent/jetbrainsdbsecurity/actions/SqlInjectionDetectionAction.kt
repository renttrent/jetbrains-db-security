package com.github.renttrent.jetbrainsdbsecurity.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import java.awt.Font
import java.util.stream.Collectors

class SqlInjectionDetectionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val editor: Editor? = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
        if (editor != null) {
            val psiFile: PsiFile? = PsiDocumentManager.getInstance(event.project!!).getPsiFile(editor.document);
            psiFile?.let { file ->
                detectSqlInjection(file)
            }
        }
    }

    private fun detectSqlInjection(file: PsiFile) {
        val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
        val elementsWithNoExpression = allElements.stream().filter {
            it.elementType.toString().contains("STRING_LITERAL_EXPRESSION") && !it.parent.elementType.toString().contains("BINARY_EXPRESSION")
        }.collect(Collectors.toList())

        val elementsWithExpression = allElements.stream().filter {
            it.elementType.toString().contains("STRING_LITERAL_EXPRESSION") && it.parent.elementType.toString().contains("BINARY_EXPRESSION")
        }.collect(Collectors.toList())

        for (element in elementsWithNoExpression) {
            println(element)
            highlightElement(file, element);
        }

        for (element in elementsWithExpression) {
            println(element)
        }
    }

    private fun highlightElement(file: PsiFile, element: PsiElement){
        val elementOffset = element.textOffset;

        val containingFile = element.containingFile

        val project: Project = containingFile.project
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val document = psiDocumentManager.getDocument(file) ?: return;
        val markupModel = DocumentMarkupModel.forDocument(document, project, true);

        val lineNumber = document.getLineNumber(elementOffset);
        val lineStartOffset = document.getLineStartOffset(lineNumber);
        val columnNumber = element.textOffset - lineStartOffset;

        val rangeHighlighter = markupModel.addRangeHighlighter(
                elementOffset,
                elementOffset + element.textLength,
                HighlighterLayer.WARNING,
                TextAttributes(null, null, JBColor.YELLOW, EffectType.WAVE_UNDERSCORE, Font.PLAIN),
                HighlighterTargetArea.EXACT_RANGE
        )
    }

    private fun isStringLiteral(element: PsiElement): Boolean {
        // Replace with the actual condition to check for a string literal in your target language
        // For example, in Kotlin, you might check if it's an instance of KtStringTemplateExpression
        return false
    }

    private fun extractStringValue(element: PsiElement): String {
        // Implement logic to extract the string value from the element
        // This will depend on the specific PSI structure of the language you're analyzing
        return ""
    }

    private fun isSusceptibleToSqlInjection(value: String): Boolean {
        // Implement your SQL injection detection logic here
        // This is where you would look for concatenations, or perhaps use of variables in constructing a SQL query
        return false
    }

    private fun reportVulnerability(element: PsiElement) {
        // Reporting logic goes here
    }
}
