package com.neelkirit.copytabpath

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class CopyTabPathService : Disposable {

    private val awtListener = AWTEventListener { event ->
        if (event !is MouseEvent || event.id != MouseEvent.MOUSE_CLICKED) return@AWTEventListener
        if (!event.isAltDown) return@AWTEventListener
        val source = event.component ?: return@AWTEventListener
        if (!isEditorTab(source)) return@AWTEventListener
        val screenPoint = Point(event.locationOnScreen)
        SwingUtilities.invokeLater { copyPathForSelectedFile(source, screenPoint) }
    }

    init {
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.MOUSE_EVENT_MASK)
    }

    /**
     * Walks up the Swing component hierarchy to check whether the click
     * landed on a TabLabel that lives inside an EditorTabs container.
     * Uses class-name matching instead of direct class references so the
     * plugin survives internal refactors across IDE versions.
     */
    private fun isEditorTab(component: Component): Boolean {
        var c: Component? = component
        var insideTabLabel = false
        while (c != null) {
            val name = c.javaClass.name
            if (name.contains("TabLabel")) insideTabLabel = true
            if (insideTabLabel && name.contains("EditorTabs")) return true
            c = c.parent
        }
        return false
    }

    private fun copyPathForSelectedFile(component: Component, screenPoint: Point) {
        val window = SwingUtilities.getWindowAncestor(component) ?: return
        val project = ProjectManager.getInstance().openProjects.firstOrNull {
            WindowManager.getInstance().getFrame(it) == window
        } ?: return

        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: return
        val basePath = project.basePath ?: return
        val relativePath = file.path.removePrefix(basePath).removePrefix("/")

        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(relativePath), null)

        val frame = WindowManager.getInstance().getFrame(project) ?: return
        val framePoint = Point(screenPoint).apply {
            translate(-frame.locationOnScreen.x, -frame.locationOnScreen.y)
        }
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Copied: <b>$relativePath</b>", MessageType.INFO, null)
            .setFadeoutTime(2000)
            .createBalloon()
            .show(RelativePoint(frame, framePoint), Balloon.Position.below)
    }

    override fun dispose() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener)
    }
}

@Suppress("DEPRECATION")
class CopyTabPathStartup : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().getService(CopyTabPathService::class.java)
    }
}
