package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.documentpro.office.business.fileviewer.dialog.BusinessPermissionDialog
import net.corekit.core.report.ReportDataManager

object BusinessPermissionDialogUtils {

    private var pendingStoragePermissionSettingsReturn = false

    fun consumePendingStoragePermissionSettingsReturn(): Boolean {
        val pending = pendingStoragePermissionSettingsReturn
        pendingStoragePermissionSettingsReturn = false
        return pending
    }

    fun showFilePermissionDialog(
        context: Context,
        needDialog: Boolean,
        needStartPermissionPage: Boolean,
        nextAction: (Boolean) -> Unit,
        onStartPermissionPageListener: (() -> Unit)? = null,
        onConfirmListener: (() -> Unit)? = null,
        onDismissListener: (() -> Unit)? = null,
        onRejectListener: (() -> Unit)? = null,
        onShowListener: (() -> Unit)? = null
    ) {

        if (XXPermissions.isGrantedPermissions(context, Permission.MANAGE_EXTERNAL_STORAGE)) {
            nextAction.invoke(true)
            return
        }

        val requestPermission = {
            ReportDataManager.reportData("permission_read_files_show",mapOf())
            BusinessSplashForegroundController.markNextIntercept()
            XXPermissions.with(context)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: List<String?>,
                        allGranted: Boolean
                    ) {
                        nextAction.invoke(allGranted)
                        ReportDataManager.reportData("permission_read_files_result",mapOf("result" to "allow"))
                    }

                    override fun onDenied(
                        permissions: List<String?>,
                        doNotAskAgain: Boolean
                    ) {
                        super.onDenied(permissions, doNotAskAgain)
                        ReportDataManager.reportData("permission_read_files_result",mapOf("result" to "deny"))
                        nextAction.invoke(false)
                        if (needStartPermissionPage && doNotAskAgain) {
                            pendingStoragePermissionSettingsReturn = true
                            onStartPermissionPageListener?.invoke()
                            XXPermissions.startPermissionActivity(context, permissions)
                        }
                    }
                })
        }
        if (needDialog) {
            BusinessPermissionDialog.show(
                context,
                onConfirmListener = {
                    onConfirmListener?.invoke()
                    requestPermission.invoke()
                },
                onDismissListener = {
                    onDismissListener?.invoke()
                },
                onRejectListener = {
                    onRejectListener?.invoke()
                    nextAction.invoke(false)
                },
                onShowListener = {
                    onShowListener?.invoke()
                })
        } else {
            requestPermission.invoke()
        }
    }

    /**
     * @param pushRequestPosition
     * Appstart=启动弹出推送权限申请
     * Scan=用户首次扫描完成点击返回弹出推送权限申请
     * Read=首次点击查看文档时弹出推送权限申请
     * SDK=SDK判断未开推送权限，弹出请求弹窗
     *
     * @param onGranted
     * 1.同意/拒绝
     * 2.不再询问/强制拒绝
     */
    fun showPostPermissionDialog(
        context: Context,
        needDialog: Boolean,
        needStartPermissionPage: Boolean,
        pushRequestPosition: String,
        onGranted: (Boolean,Boolean) -> Unit
    ) {
        BusinessPointLog.pushRequest(pushRequestPosition)
        onGranted.invoke(true, false)
        BusinessPointLog.pushResult(result = "allow1", position = pushRequestPosition)
    }

}
