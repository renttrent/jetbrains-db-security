package com.github.renttrent.jetbrainsdbsecurity.actions

import com.github.renttrent.jetbrainsdbsecurity.services.*
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
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import java.awt.Font
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


        val elementsWithExpression = elementsToExclude.stream().collect(Collectors.groupingBy { x-> x.parent.elementType.toString() })

        val targetElements = allElements.stream().filter {
            it.elementType.toString().contains("TARGET_EXPRESSION")
        }.collect(Collectors.toList())

        val pattern = "\\{[\\w\\d]*\\}|\\{(.*?)\\}|%\\w+".toRegex()
        for (map in elementsWithExpression) {
            for (elem in map.value) {
                val (variableList: MutableList<String>, concatQuery) = concatLiteral(elem, targetElements, pattern)

                var validSql: Boolean = isValidSql(variableList, concatQuery)

                if (!validSql) continue
                val sqlVulnerable = isSqlVulnerable(concatQuery)
                if(sqlVulnerable != WarningSeverity.OK){
                    highlightElement(file, elem, sqlVulnerable)
                }
            }
        }
    }

    private fun isValidSql(variableList: MutableList<String>, concatQuery: String): Boolean {
        var validSql: Boolean = false
        validSql = if (variableList.contains("{}")) {
            isValidSqlNaive(concatQuery)
        } else {
            SQLParserUtil().isValid(concatQuery)
        }
        return validSql
    }

    private fun concatLiteral(elem: PsiElement, targetElements: MutableList<PsiElement>, pattern: Regex): Pair<MutableList<String>, String> {
        val list = findReferenceExpressions(elem.parent)
        val variableList: MutableList<String> = mutableListOf()

        for (refElem in list) {
            val value = findTargetReference(refElem.reference!!, targetElements) ?: break
            variableList += value
        }

        // Replace function
        val iterator = variableList.iterator()

        // Replace function
        val concatQuery = removeQuotesAndF(pattern.replace(elem.text) {
            if (iterator.hasNext()) iterator.next() else it.value
        })
        return Pair(variableList, concatQuery)
    }

    private fun removeQuotesAndF(input: String): String {
        var result = input.removePrefix("f").removePrefix("F")

        if ((result.startsWith("\"") && result.endsWith("\"")) || (result.startsWith("'") && result.endsWith("'"))) {
            result = result.substring(1, result.length - 1)
        }

        return result
    }

    private fun findTargetReference(ref: PsiReference, targetElements: MutableList<PsiElement>): @NlsSafe String? {
        val target = findReference(ref, targetElements)
        if(target === null) return null
        val parent = target.parent
        return if(parent.children.last().elementType.toString().contains("STRING_LITERAL_EXPRESSION")) {
            val originalString = parent.children.last().text
            if (originalString.startsWith("\"") && originalString.endsWith("\"")) {
                originalString.substring(1, originalString.length - 1)
            } else {
                originalString
            }
        } else "{}"
    }

    private fun findReference(ref: PsiReference, targets: MutableList<PsiElement>): PsiElement? {
        for (target in targets) {
            if(ref.isReferenceTo(target)) {
                return target
            }
        }
        return null
    }

    private fun findReferenceExpressions(element: PsiElement) : MutableList<PsiElement>{
        val list : MutableList<PsiElement> = mutableListOf<PsiElement>()
        return findReferenceExpressions(element, list);
    }

    private fun findReferenceExpressions(element: PsiElement, list : MutableList<PsiElement>) : MutableList<PsiElement>{
        //special case for fString
        if(element.elementType.toString().contains("REFERENCE_EXPRESSION"))
        {
            return findReferenceExpressions(element.nextSibling);
        }

        for (children in element.children){
            if(children.elementType.toString().contains("REFERENCE_EXPRESSION"))
                list.add(children)
            if(children.children.isNotEmpty())
                list += findReferenceExpressions(children)
        }
        return list
    }

    private fun highlightElement(file: PsiFile, element: PsiElement, severity: WarningSeverity){
        val elementOffset = element.textOffset

        val containingFile = element.containingFile

        val project: Project = containingFile.project
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val document = psiDocumentManager.getDocument(file) ?: return
        val markupModel = DocumentMarkupModel.forDocument(document, project, true)

        val lineNumber = document.getLineNumber(elementOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val columnNumber = element.textOffset - lineStartOffset


        var warning = ""

        if (severity === WarningSeverity.WEAK) warning = "WEAK WARNING"
        if (severity === WarningSeverity.STRONG) warning = "STRONG WARNING"

        showNotification(project, warning, "(${lineNumber}, ${columnNumber}) Element - " + element.text.toString() + " not valid form of query")
        markupModel.addRangeHighlighter(
                elementOffset,
                elementOffset + element.textLength,
                HighlighterLayer.WARNING,
                TextAttributes(null, null, JBColor.ORANGE, EffectType.ROUNDED_BOX, Font.ITALIC),
                HighlighterTargetArea.EXACT_RANGE
        )
    }

}
