package com.github.renttrent.jetbrainsdbsecurity.listeners

import com.github.renttrent.jetbrainsdbsecurity.actions.SqlInjectionDetectionAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectForFile
import com.intellij.openapi.vfs.VirtualFile


class SaveListener : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val file: VirtualFile? = FileDocumentManager.getInstance().getFile(document)
        val project: Project? = file?.let { guessProjectForFile(it) }

        SqlInjectionDetectionAction().parseDocument(project!!, document)
    }
}

