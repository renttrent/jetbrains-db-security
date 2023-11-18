package com.github.renttrent.jetbrainsdbsecurity.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class SqlInjectionDetectionAllFilesAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val pythonFiles = FileTypeIndex.getFiles(FileTypeManager.getInstance().findFileTypeByName("Python"), GlobalSearchScope.projectScope(project))

        for (file in pythonFiles) {
            // double check
            if (isPythonFile(file)) {
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: continue

                println(psiFile.name)
                SqlInjectionDetectionAction().detectSqlInjection(psiFile)
            }
        }
    }

    private fun isPythonFile(file: VirtualFile): Boolean {
        return file.extension.toString() == "py"
    }
}