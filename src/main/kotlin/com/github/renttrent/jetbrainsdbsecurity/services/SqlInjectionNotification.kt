package com.github.renttrent.jetbrainsdbsecurity.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

fun showNotification(project: Project, title: String, content: String) {
    val notification = Notification(
        "Sql Injection Notification",
        title,
        content,
        NotificationType.INFORMATION
    )
    Notifications.Bus.notify(notification, project)
}