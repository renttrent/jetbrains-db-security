package com.github.renttrent.jetbrainsdbsecurity.actions

import com.github.renttrent.jetbrainsdbsecurity.services.SQLParserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
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
import java.util.Dictionary
import java.util.stream.Collectors
class SqlInjectionDetectionAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val editor: Editor? = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
        if (editor != null) {
            parseDocument(editor.project!!, editor.document)
        }
    }

    fun parseDocument(project: Project, document: Document) {
        val psiFile: PsiFile? = PsiDocumentManager.getInstance(project).getPsiFile(document)
        psiFile?.let { file ->
            detectSqlInjection(file)
        }
    }

    fun detectSqlInjection(file: PsiFile) {
        val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)

        val allStringLiterals = allElements
                .stream()
                .filter {
                    it.elementType.toString().contains("STRING_LITERAL_EXPRESSION")
                }.collect(Collectors.toList())

        val elementsToExclude = allStringLiterals
                .stream().filter {
                    it.parent.elementType.toString().contains("BINARY_EXPRESSION")
                            || it.parent.elementType.toString().contains("REFERENCE_EXPRESSION")
                            || (it.children.isNotEmpty() && it.children[0].elementType.toString().contains("FSTRING_NODE"))
                }.collect(Collectors.toList())

        val elementsWithNoExpression = allStringLiterals.stream()
                .filter {
                    !elementsToExclude.contains(it)
                }.collect(Collectors.toList());


        val elementsWithExpression = elementsToExclude.stream().collect(Collectors.groupingBy { x-> x.parent.elementType.toString() })

        val dict : Dictionary<PsiElement, List<String>>

        //BinaryExpr
        for (map in elementsWithExpression) {
            println("\nFucked up Strings with "+ map.key)
            for(element in map.value){
                println(element.toString())
            }
        }
    }

    private fun parseStringsWithExpression(type: String, element: PsiElement){
        if(type == "BINARY_EXPRESSION"){
            concatBinaryExpr()
        }else if(type == "REFERENCE_EXPRESSION"){
            concatReferenceExpr()
        }else if(type == "ASSIGNMENT_STATEMENT"){
            concaAssignmentStatement() //fString
        }
    }

    private fun concatReferenceExpr() {
        TODO("Not yet implemented")
    }

    private fun concatBinaryExpr() {
        TODO("Not yet implemented")
    }

    private fun concaAssignmentStatement() {
        TODO("Not yet implemented")
    }


    private fun highlightElement(file: PsiFile, element: PsiElement){
        val elementOffset = element.textOffset

        val containingFile = element.containingFile

        val project: Project = containingFile.project
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val document = psiDocumentManager.getDocument(file) ?: return
        val markupModel = DocumentMarkupModel.forDocument(document, project, true)

        val lineNumber = document.getLineNumber(elementOffset)
        //val lineStartOffset = document.getLineStartOffset(lineNumber)
        //val columnNumber = element.textOffset - lineStartOffset

        markupModel.addRangeHighlighter(
                elementOffset,
                elementOffset + element.textLength,
                HighlighterLayer.WARNING,
                TextAttributes(null, JBColor.LIGHT_GRAY, JBColor.YELLOW, EffectType.WAVE_UNDERSCORE, Font.PLAIN),
                HighlighterTargetArea.EXACT_RANGE
        )
    }

}
