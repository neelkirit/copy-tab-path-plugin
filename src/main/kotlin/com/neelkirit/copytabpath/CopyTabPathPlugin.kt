package com.neelkirit.copytabpath

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.JList
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class CopyTabPathService : Disposable {

    private val awtListener = AWTEventListener { event ->
        if (event !is MouseEvent || event.id != MouseEvent.MOUSE_CLICKED) return@AWTEventListener
        if (!event.isAltDown) return@AWTEventListener
        val source = event.component ?: return@AWTEventListener
        val screenPoint = Point(event.locationOnScreen)
        val clickPoint = Point(event.x, event.y)
        event.consume()
        SwingUtilities.invokeLater { handleOptionClick(source, clickPoint, screenPoint) }
    }

    init {
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.MOUSE_EVENT_MASK)
    }

    private fun handleOptionClick(component: Component, clickPoint: Point, screenPoint: Point) {
        val file = resolveFile(component, clickPoint) ?: return
        val project = resolveProject(component) ?: return
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

    private fun resolveFile(component: Component, clickPoint: Point): VirtualFile? {
        // For JTree, select the node under the cursor before querying DataContext
        if (component is JTree) {
            val treePath = component.getPathForLocation(clickPoint.x, clickPoint.y)
            if (treePath != null) {
                component.selectionPath = treePath
                val node = treePath.lastPathComponent as? DefaultMutableTreeNode
                val userObj = node?.userObject
                if (userObj is VirtualFile) return userObj
            }
        }

        // For JList, select the item under the cursor before querying DataContext
        if (component is JList<*>) {
            val index = component.locationToIndex(clickPoint)
            if (index >= 0) {
                component.selectedIndex = index
                val item = component.model.getElementAt(index)
                if (item is VirtualFile) return item
            }
        }

        // Fall back to IntelliJ's DataContext which works for editor tabs, Project view, etc.
        val dataContext = DataManager.getInstance().getDataContext(component)
        return dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    }

    private fun resolveProject(component: Component): Project? {
        val dataContext = DataManager.getInstance().getDataContext(component)
        dataContext.getData(PlatformDataKeys.PROJECT)?.let { return it }

        val window = SwingUtilities.getWindowAncestor(component) ?: return null
        return ProjectManager.getInstance().openProjects.firstOrNull {
            WindowManager.getInstance().getFrame(it) == window
        }
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
