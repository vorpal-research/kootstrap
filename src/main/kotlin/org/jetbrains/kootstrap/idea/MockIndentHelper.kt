package org.jetbrains.kootstrap.idea

import com.intellij.lang.ASTNode
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.codeStyle.IndentHelper

/**
 * Created by belyaev on 7/15/16.
 */

class MockIndentHelper : IndentHelper() {
    override fun getIndent(
            project: Project?,
            fileType: FileType?,
            element: ASTNode?) = 0

    override fun getIndent(
            project: Project?,
            fileType: FileType?,
            element: ASTNode?,
            includeNonSpace: Boolean) = 0
}
