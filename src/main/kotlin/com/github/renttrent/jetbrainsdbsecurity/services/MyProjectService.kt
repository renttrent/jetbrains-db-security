package com.github.renttrent.jetbrainsdbsecurity.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.renttrent.jetbrainsdbsecurity.MyBundle
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.psi.PsiDocumentManager
import org.jdesktop.swingx.action.ActionManager

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {


    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }
}
