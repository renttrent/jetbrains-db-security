package com.github.renttrent.jetbrainsdbsecurity.listeners;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;

import org.jetbrains.annotations.NotNull;



public final class SaveListener implements FileDocumentManagerListener {
    @Override
    public void beforeAllDocumentsSaving(){
        //println("before all documents Save");
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        //println("before document save " + document.getClass().toString());
    }

}
