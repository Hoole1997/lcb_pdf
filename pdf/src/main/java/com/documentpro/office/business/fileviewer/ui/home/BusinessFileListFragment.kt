package com.documentpro.office.business.fileviewer.ui.home

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighLight
import com.app.hubert.guide.model.RelativeGuide
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.adapter.BusinessFileAdapter
import com.documentpro.office.business.fileviewer.base.BaseLazyFragment
import com.documentpro.office.business.fileviewer.databinding.FragmentFileListBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import com.documentpro.office.business.fileviewer.dialog.BusinessSortBottomSheetDialog
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessSortType
import com.documentpro.office.business.fileviewer.dialog.BusinessFileDetailDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileLockDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileMoreDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileRemakeNameDialog
import com.documentpro.office.business.fileviewer.ui.pdf.ImagePreviewActivity
import com.documentpro.office.business.fileviewer.utils.BusinessShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.documentpro.office.business.fileviewer.utils.BusinessPrintUtils
import com.google.android.material.button.MaterialButton
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.ui.pdf.MergePdfActivity
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDialogUtils
import com.documentpro.office.business.fileviewer.utils.BusinessRefreshManager
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.BusinessGuideCallbackController
import com.documentpro.office.business.fileviewer.utils.LinearTopSmoothScroller
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import net.corekit.core.ext.isDefaultLauncher

class BusinessFileListFragment : BaseLazyFragment<FragmentFileListBinding, BusinessHomeModel>() {

    companion object {
        const val TAG = "BusinessFileListFragment"

        /**
         * 创建普通浏览模式的 Fragment
         */
        fun newInstance(
            queryType: String = BusinessFileType.PDF.name,
            chooseMode: Boolean = false
        ): BusinessFileListFragment {
            val config = if (chooseMode) {
                BusinessFileListConfig.choose(queryType)
            } else {
                BusinessFileListConfig.normal(queryType)
            }
            return createFragment(config)
        }

        /**
         * 创建PDF锁定管理模式的 Fragment
         */
        fun newInstanceFromPDF(lockType: Int): BusinessFileListFragment {
            val lockFilter = when (lockType) {
                1 -> BusinessFileListConfig.LockFilter.LOCKED_ONLY
                2 -> BusinessFileListConfig.LockFilter.UNLOCKED_ONLY
                else -> BusinessFileListConfig.LockFilter.ALL
            }
            val config = BusinessFileListConfig.pdfLock(lockFilter)
            return createFragment(config)
        }

        /**
         * 创建打印模式的 Fragment
         */
        fun newInstanceFromPrint(): BusinessFileListFragment {
            val config = BusinessFileListConfig.print()
            return createFragment(config)
        }

        /**
         * 创建最近文件模式的 Fragment
         */
        fun newInstanceForRecent(queryType: String = BusinessFileType.PDF.name): BusinessFileListFragment {
            val config = BusinessFileListConfig.recent(queryType)
            return createFragment(config)
        }

        /**
         * 创建收藏文件模式的 Fragment
         */
        fun newInstanceForFavorite(queryType: String = BusinessFileType.PDF.name): BusinessFileListFragment {
            val config = BusinessFileListConfig.favorite(queryType)
            return createFragment(config)
        }

        /**
         * 通用的Fragment创建方法
         */
        fun createFragment(config: BusinessFileListConfig): BusinessFileListFragment {
            return BusinessFileListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(BusinessFileListConfig.BUNDLE_KEY, config)
                }
            }
        }

        /**
         * 便捷方法：使用配置类创建Fragment
         */
        fun create(config: BusinessFileListConfig): BusinessFileListFragment {
            return createFragment(config)
        }
    }

    /** Fragment 配置 */
    private lateinit var config: BusinessFileListConfig
    private var selectedSortType = BusinessSortType.MODIFIED_DESC
    private var allFiles: List<BusinessFileInfo> = emptyList()
    private var currentKeyword: String = ""

    private lateinit var fileAdapter: BusinessFileAdapter
    private lateinit var mainModel: BusinessMainModel
    private lateinit var chooseModel: BusinessChooseModel
    private lateinit var refreshManager: BusinessRefreshManager
    private var guideController: Controller? = null

    // 防重复点击相关变量
    private var lastClickTime = 0L
    private val clickInterval = 300 // 300毫秒间隔

    private var scrollToTop: Boolean = false

    override fun initBinding(): FragmentFileListBinding {
        return FragmentFileListBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 从 Bundle 中获取配置，如果没有则使用默认配置
        try {
            config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelable(BusinessFileListConfig.BUNDLE_KEY, BusinessFileListConfig::class.java) ?: BusinessFileListConfig.normal()
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelable(BusinessFileListConfig.BUNDLE_KEY) ?: BusinessFileListConfig.normal()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BusinessFileListConfig.normal()
        }

        fileAdapter = BusinessFileAdapter(config.isChooseMode)
        binding.rvFile.adapter = fileAdapter
        fileAdapter.addOnItemChildClickListener(R.id.iv_more) { _, _, position ->
            val item = fileAdapter.getItem(position)
            // 过滤掉广告占位符
            if (item is BusinessFileInfo && !item.isAd()) {
                safetyContext()?.let { safetyContext ->
                    BusinessFileMoreDialog(safetyContext, item, mainModel)
                        .setOnRenameClickListener {
                            showFileRenameDialog(item, position)
                        }
                        .setOnFileLockClickListener {
                            showFileLockDialog(safetyContext, item, position, true)
                        }
                        .setOnDetailClickListener {
                            showFileDetailDialog(safetyContext, item)
                        }
                        .setOnShareClickListener {
                            BusinessShareUtils.share(safetyContext, item)
                        }
                        .setOnFileDeleteClickListener {
                            delete(safetyContext, item)
                        }
                        .show()
                }
            }
        }
        fileAdapter.setOnItemClickListener { _, _, position ->
            // 防重复点击，间隔1秒
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= clickInterval) {
                lastClickTime = currentTime
                val item = fileAdapter.getItem(position)
                // 过滤掉广告占位符
                if (item is BusinessFileInfo && !item.isAd()) {
                    safetyContext()?.let { ctx ->
                        // 直接打开文件，不再在点击时申请通知权限（避免 XXPermissions 回调缓存问题）
                        clickLaunch(ctx, position, item)
                    }
                }
            }
        }
        fileAdapter.isStateViewEnable = true
        safetyContext()?.let {
            fileAdapter.setStateViewLayout(it, R.layout.layout_empty)
        }

        binding.refreshLayout.setOnRefreshListener {
            refresh()
            binding.refreshLayout.finishRefresh(1500)
        }
        // 初始化刷新管理器
        initRefreshManager()

        // 对于某些特殊模式，立即刷新数据
        if (config.isPrintMode || config.isChooseMode || !config.showAllFiles) {
//            refresh()
        }
    }

    private fun clickLaunch(context: Context, position: Int, item: BusinessFileInfo) {
        when {
            config.isPrintMode -> handlePrintMode(context, item)
            config.isChooseMode -> if (item.isLocked) {
                handleNormalMode(context,position,item)
            } else {
                handleChooseMode(position, item)
            }
            else -> handleNormalMode(context, position, item)
        }
    }

    /**
     * 处理打印模式的文件点击
     */
    private fun handlePrintMode(context: Context, item: BusinessFileInfo) {
        BusinessPrintUtils.printFile(context, item)
    }

    /**
     * 处理选择模式的文件点击
     */
    private fun handleChooseMode(position: Int, item: BusinessFileInfo) {
        // 创建新的FileInfo对象，避免直接修改原对象
        val updatedItem = item.copy().apply {
            select = !item.select
        }

        // 创建新的列表，替换对应位置的项目
        val currentList = fileAdapter.items.toMutableList()
        currentList[position] = updatedItem

        // 通过submitList触发DiffUtil更新
        fileAdapter.submitList(currentList.toList())

        // 提交选择的文件列表（过滤掉广告占位符）
        chooseModel.submitFileList(currentList.filterIsInstance<BusinessFileInfo>().filter { !it.isAd() })
    }

    /**
     * 处理普通模式的文件点击
     */
    private fun handleNormalMode(context: Context, position: Int, item: BusinessFileInfo) {
        when {
            item.isLocked -> {
                showFileLockDialog(context, item, position, false)
                return
            }

            config.showUnlockedOnly -> {
                showFileLockDialog(context, item, position, true)
                return
            }

            else -> {
                openFileWithAd(context, position, item)
            }
        }
    }

    /**
     * 带广告的文件打开逻辑
     */
    private fun openFileWithAd(context: Context, position: Int, item: BusinessFileInfo) {
        val openAction = {
            // 只有从文件系统数据源打开文件时才保存到最近列表
            if (config.isFileSystemSource) {
                mainModel.addToRecentAndRefresh(item)
            }

            if (config.queryType == BusinessFileType.IMAGE.name) {
                ImagePreviewActivity.launch(
                    context,
                    ArrayList<BusinessFileInfo>(fileAdapter.items.filterIsInstance<BusinessFileInfo>().filter { !it.isAd() }),
                    position
                )
            } else {
                openFile(context, item)
            }
        }
        if (safetyContext()!=null) {
            openAction.invoke()
        }
    }

    override fun lazyLoad() {
        super.lazyLoad()
        Log.d(TAG, "lazyLoad ${config.queryType}")
//        refresh()
    }

    private fun refresh() {
        safetyContext()?.let { context ->
            try {
                // 根据数据源类型执行相应的查询
                when (config.dataSource) {
                    BusinessFileListConfig.DataSource.RECENT -> {
                        model?.queryRecentFileList(config.queryType, selectedSortType)
                    }

                    BusinessFileListConfig.DataSource.FAVORITE -> {
                        model?.queryFavoriteFileList(config.queryType, selectedSortType)
                    }

                    BusinessFileListConfig.DataSource.FILE_SYSTEM -> {
                        // 文件系统数据源需要检查权限
                        if (hasFileSystemPermission(context)) {
                            removePermissionFooter()
                            model?.queryFileList(config.queryType, selectedSortType)
                        } else {
                            // IMAGE类型无权限时，使用适配器StateView显示权限页面
                            if (config.queryType == BusinessFileType.IMAGE.name) {
                                showPermissionStatePage()
                            } else {
                                // 其他类型无权限时显示Demo文件和引导
                                model?.queryDemoFileList(config.queryType, selectedSortType)
                                // 非All类型时，在列表底部显示权限请求视图
                                if (config.queryType != BusinessFileType.All.name) {
                                    showPermissionFooter()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                BusinessPointLog.logEvent("Exception", mapOf("reason" to "${e.message}"))
            } finally {
                binding.refreshLayout.finishRefresh()
            }
        }
    }

    /**
     * 检查是否有文件系统访问权限
     */
    private fun hasFileSystemPermission(context: Context): Boolean {
        return XXPermissions.isGrantedPermissions(context, Permission.MANAGE_EXTERNAL_STORAGE)
    }

    private fun showPermissionStatePage() {
        BusinessPointLog.logEvent(
            "All_File_Request_Show", mapOf(
                "File_Request_Position" to 4
            )
        )
        fileAdapter.isStateViewEnable = true
        safetyContext()?.let { safetyContext ->
            fileAdapter.setStateViewLayout(safetyContext, R.layout.layout_permission_file)
            // 设置顶部 margin 100dp
            fileAdapter.stateView?.let { stateView ->
                val layoutParams = stateView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                layoutParams?.topMargin = (100 * safetyContext.resources.displayMetrics.density).toInt()
                stateView.layoutParams = layoutParams
            }
            fileAdapter.stateView?.findViewById<MaterialButton>(R.id.btn_continue)
                ?.setOnClickListener {
                    BusinessPointLog.logEvent(
                        "All_File_Click", mapOf(
                            "File_Request_Position" to 4
                        )
                    )
                    BusinessPermissionDialogUtils.showFilePermissionDialog(
                        safetyContext,
                        needDialog = false,
                        needStartPermissionPage = true,
                        nextAction = {
                            if (it) {
                                BusinessPointLog.logEvent(
                                    "All_File_Success", mapOf(
                                        "File_Request_Position" to 4
                                    )
                                )
                            }
                        }
                    )
                }
        }
    }

    /**
     * 在列表底部显示权限请求视图（带渐变动画）
     */
    private fun showPermissionFooter() {
        val footerView = binding.permissionFooter.root
        footerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = SizeUtils.dp2px(110f) - if(requireContext().isDefaultLauncher()) (BarUtils.getStatusBarHeight() + SizeUtils.dp2px(10f) ) else 0
        }
        if (footerView.visibility == android.view.View.VISIBLE) {
            return
        }
        footerView.alpha = 0f
        footerView.visibility = android.view.View.VISIBLE
        footerView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        safetyContext()?.let { safetyContext ->
            binding.permissionFooter.root.findViewById<MaterialButton>(R.id.btn_continue)?.setOnClickListener {
                BusinessPointLog.logEvent(
                    "All_File_Click", mapOf(
                        "File_Request_Position" to 4
                    )
                )
                BusinessPermissionDialogUtils.showFilePermissionDialog(
                    safetyContext,
                    needDialog = false,
                    needStartPermissionPage = true,
                    nextAction = {
                        if (it) {
                            BusinessPointLog.logEvent(
                                "All_File_Success", mapOf(
                                    "File_Request_Position" to 4
                                )
                            )
                        }
                    }
                )
            }

            BusinessPointLog.logEvent(
                "All_File_Request_Show", mapOf(
                    "File_Request_Position" to 4
                )
            )
        }
    }

    /**
     * 移除底部权限请求视图（带渐变动画）
     */
    private fun removePermissionFooter() {
        val footerView = binding.permissionFooter.root
        if (footerView.visibility == android.view.View.GONE) {
            return
        }
        footerView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                footerView.visibility = android.view.View.GONE
            }
            .start()
    }

    override fun initViewModel(): BusinessHomeModel? {
        mainModel = activityViewModels<BusinessMainModel>().value
        chooseModel = activityViewModels<BusinessChooseModel>().value

        return viewModels<BusinessHomeModel>().value
    }

    private fun initRefreshManager() {
        refreshManager = BusinessRefreshManager(
            lifecycleOwner = this,
            mainModel = mainModel,
            config = BusinessRefreshManager.RefreshConfig(
                dataSource = config.dataSource.value,
                onRefresh = {
                    // 只在Fragment可见且已加载时执行刷新
                    if (shouldRefreshNow()) {
                        refresh()
                    } else {

                    }
                }
            )
        )
        refreshManager.startObserving()
    }

    private fun shouldRefreshNow(): Boolean {
        return when {
            // Activity场景：立即刷新（单Fragment直接嵌入）
            parentFragment == null -> true
            // ViewPager场景：只有可见且已Resume时才刷新
            parentFragment != null -> isVisible && isResumed
            else -> false
        }
    }

    /**
     * 比较两个文件列表是否相同（用于避免不必要的刷新）
     */
    private fun isSameFileList(oldList: List<BusinessFileInfo>, newList: List<BusinessFileInfo>): Boolean {
        if (oldList.size != newList.size) return false
        return oldList.zip(newList).all { (old, new) ->
            old.path == new.path &&
            old.name == new.name &&
            old.size == new.size &&
            old.isLocked == new.isLocked &&
            old.dateCreated == new.dateCreated
        }
    }

    override fun initObserve() {
        // 置顶
        mainModel.refreshHomeModel.observe(this) {
            scrollToTop = true
        }
        model?.fileInfoEvent?.observe(this) { newFiles ->
            binding.refreshLayout.finishRefresh()
            val filteredList = when (config.lockFilter) {
                BusinessFileListConfig.LockFilter.LOCKED_ONLY -> newFiles.filter { file -> file.isLocked }
                BusinessFileListConfig.LockFilter.UNLOCKED_ONLY -> newFiles.filter { file -> !file.isLocked }
                BusinessFileListConfig.LockFilter.ALL -> newFiles
            }

            // 比较数据源是否一致，避免不必要的刷新
            if (!isSameFileList(allFiles, filteredList)) {
                allFiles = filteredList
                filterByKeyword(currentKeyword)
            }

            if(scrollToTop){
                scrollToTop = false
                binding.rvFile.postDelayed({
                    scrollItemToTop(0)
                },500)
            }
        }

        chooseModel.allChooseEvent.observe(this) {
            val list = fileAdapter.items.toMutableList()
            var hasChanges = false
            list.forEachIndexed { index, item ->
                // 过滤掉广告占位符，只处理真实文件
                if (item is BusinessFileInfo && !item.isAd() && item.select != it) {
                    // 创建新的FileInfo对象，避免直接修改原对象
                    val updatedItem = item.copy().apply {
                        select = it
                    }
                    list[index] = updatedItem
                    hasChanges = true
                }
            }
            // 只有在有变化时才提交新列表
            if (hasChanges) {
                fileAdapter.submitList(list.toList())
                chooseModel.submitFileList(list.filterIsInstance<BusinessFileInfo>().filter { !it.isAd() })
            }
        }
    }

    private fun openFile(context: Context, fileInfo: BusinessFileInfo) {
        if (activity == null) {
            return
        }
        requireActivity().loadInterstitial {
            fileInfo.open(context)
        }
    }

//    fun launchChoose() {
//        safetyContext()?.let { safetyContext ->
//            try {
//                startActivity(ChooseFileActivity.launchIntent(
//                    safetyContext,
//                    config.queryType
//                ))
//            } catch (e: IllegalStateException) {
//                LogUtils.e("ActivityResultLauncher not registered or already destroyed", e)
//            }
//        }
//    }


    fun showSortDialog() {
        safetyContext()?.let { safetyContext ->
            BusinessSortBottomSheetDialog(safetyContext, selectedSortType) { type ->
                selectedSortType = type
                try {
                    refresh()
                    binding.rvFile.postDelayed({
                        binding.rvFile.scrollToPosition(0)
                    },200)
                } catch (e: Exception) {
                    BusinessPointLog.logEvent("Exception", mapOf("reason" to "${e.message}"))
                }
            }.show()
        }
    }

    fun showFileRenameDialog(fileInfo: BusinessFileInfo, position: Int) {
        safetyContext()?.let { safetyContext ->
            BusinessFileRemakeNameDialog.show(safetyContext, fileInfo, position) { updatedFileInfo, mPosition ->
                try {
                    // 更新当前列表中的文件信息
                    val currentList = fileAdapter.items.toMutableList()
                    if (mPosition >= 0 && mPosition < currentList.size && currentList[mPosition] is BusinessFileInfo) {
                        currentList[mPosition] = updatedFileInfo

                        // 提交新列表，触发DiffUtil比较
                        fileAdapter.submitList(currentList.toList())

                        // 同时更新allFiles列表，确保搜索和筛选功能正常
                        val fileIndex = allFiles.indexOfFirst { it.path == fileInfo.path }
                        if (fileIndex >= 0) {
                            allFiles = allFiles.toMutableList().apply {
                                set(fileIndex, updatedFileInfo)
                            }
                        }

                        LogUtils.d("FileRename", "文件重命名成功: ${fileInfo.name} -> ${updatedFileInfo.name}")
                    }

                    // 通知文件系统发生变化，触发全局刷新
                    mainModel.notifyFileSystemChanged()
                } catch (e: Exception) {
                    LogUtils.e("FileRename", "重命名后更新列表失败: ${e.message}")
                    // 如果更新失败，执行一次完整刷新
                    refresh()
                }
            }
        }
    }

    fun showFileLockDialog(context: Context, fileInfo: BusinessFileInfo, position: Int, isLock: Boolean) {
        BusinessFileLockDialog.show(context, isLock) { password ->
            performFileLockOperation(fileInfo, position, isLock, password)
        }
    }

    /**
     * 执行文件加锁/解锁操作
     */
    private fun performFileLockOperation(
        fileInfo: BusinessFileInfo,
        position: Int,
        isLock: Boolean,
        password: String
    ) {
        lifecycleScope.launch {
            val success = try {
                if (isLock) {
                    BusinessPointLog.logEvent("LockPDF_Fnish")
                    BusinessPdfUtils.encryptPdfInPlace(fileInfo.path, password, password)
                } else {
                    BusinessPointLog.logEvent("UnlockPDF_Fnish")
                    BusinessPdfUtils.decryptPdfInPlace(fileInfo.path, password)
                }
            } catch (e: Exception) {
                Log.e(TAG, "文件${if (isLock) "加锁" else "解锁"}失败: ${e.message}")
                false
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    // 创建新的文件对象并更新文件状态
                    val updatedFileInfo = fileInfo.copy(isLocked = isLock)

                    // 使用DiffUtil更新方式替换原来的set方法
                    val currentList = fileAdapter.items.toMutableList()
                    if (position >= 0 && position < currentList.size) {
                        currentList[position] = updatedFileInfo
                        fileAdapter.submitList(currentList.toList())
                    }

                    // 更新最近列表和收藏列表中的锁定状态
                    BusinessRecentStorage.updateFileLockStatus(fileInfo.path, isLock)

//                    refresh()
                    // 显示广告
                } else {
                    ToastUtils.showShort(getString(R.string.dialog_password_incorrect))
                }
            }
        }
    }

    fun showFileDetailDialog(context: Context, fileInfo: BusinessFileInfo) {
        BusinessFileDetailDialog.show(context, fileInfo)
    }

    fun delete(context: Context, item: BusinessFileInfo) {
        showDeleteConfirmDialog(context, item) {
            performFileDelete(item)
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(context: Context, item: BusinessFileInfo, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.choose_file_delete_confirm_title))
            .setMessage(getString(R.string.choose_file_delete_single_confirm_message, item.name))
            .setNegativeButton(getString(R.string.choose_file_delete_confirm_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.choose_file_delete_confirm_ok)) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 执行文件删除操作
     */
    private fun performFileDelete(item: BusinessFileInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (FileUtils.delete(item.path)) {
                withContext(Dispatchers.Main) {
                    // 从适配器中移除已删除的文件（保留广告占位符）
                    val newList = fileAdapter.items.filterIsInstance<BusinessFileInfo>()
                        .filter { it.isAd() || it.path != item.path }
                    fileAdapter.submitList(newList)

                    // 通知文件系统发生变化，触发全局刷新
                    mainModel.notifyFileSystemChanged()
                }
            }
        }
    }

    fun filterByKeyword(keyword: String) {
        try {
            val oldKeyword = currentKeyword
            currentKeyword = keyword

            // 只有关键词变化时才禁用动画
            if (oldKeyword != keyword) {
                fileAdapter.animationEnable = keyword.isBlank()
            }

            val newList: MutableList<BusinessFileInfo> = if (keyword.isBlank()) {
                allFiles.toMutableList()
            } else {
                allFiles.filter { it.name.contains(keyword) }.toMutableList()
            }

            // 在非选择模式、非搜索状态下添加广告
            val condition1 = !config.isChooseMode && keyword.isBlank() && newList.isNotEmpty()
            val condition2 = requireActivity().javaClass.simpleName == MergePdfActivity::class.java.simpleName
            if (condition1 || condition2) {
                val adPosition = calculateAdPosition(newList.size)
                if (adPosition <= newList.size) {
                    newList.add(adPosition, BusinessFileInfo.createAdPlaceholder())
                }
            }

            fileAdapter.submitList(newList)
            fileAdapter.setHighlightKeyword(keyword)
            showClickGuide()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun showClickGuide() {
        if (guideController != null) {
            return
        }
        if (config.queryType != "All") {
            return
        }
        // 安全检查：确保 Fragment 已附加且 View 存在
        if (!isAdded || activity == null || view == null) {
            return
        }
        binding.rvFile.postDelayed({
            // 延迟回调中再次检查状态
            if (!isAdded || activity == null || view == null) {
                return@postDelayed
            }
            fileAdapter.items.forEachIndexed { index, item ->
                // 过滤掉广告占位符
                if (item is BusinessFileInfo && !item.isAd()) {
                    if (item.name.contains("Demo.pdf") && guideController == null) {
                        val viewHolder = binding.rvFile.findViewHolderForAdapterPosition(index)
                            ?: return@postDelayed
                        // 检查 viewHolder.itemView 的父布局是否存在
                        if (viewHolder.itemView.parent == null) {
                            return@postDelayed
                        }
                        safetyContext()?.let { ctx ->
                            try {
                                guideController = NewbieGuide.with(requireActivity())
                                    .setLabel("guide1")
                                    .setShowCounts(1)
                                    .anchor(parentFragment?.parentFragment?.view ?: requireActivity().findViewById<View>(android.R.id.content))
                                    .addGuidePage(
                                        GuidePage.newInstance()
                                            .addHighLight(
                                                viewHolder.itemView, HighLight.Shape.RECTANGLE,RelativeGuide(
                                                    R.layout.layout_doc_guide_click,
                                                    Gravity.BOTTOM
                                                )
                                            )
                                            .setLayoutRes(R.layout.layout_doc_guide_click_empty)
                                            .setEverywhereCancelable(false)
                                            .setOnLayoutInflatedListener { view, controller ->
                                                view.findViewById<TextView>(R.id.tv_skip).setOnClickListener{
                                                    controller.remove()
                                                    BusinessGuideCallbackController.notifySkipped()
                                                }
                                                view.setOnClickListener {
                                                    safetyContext()?.let {
                                                        controller.remove()
                                                        BusinessGuideCallbackController.markOpenedFromGuide()
                                                        clickLaunch(it,index,item)
                                                    }
                                                }
                                            }
                                    )
                                    .show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }, 500)
    }

    fun scrollItemToTop(position: Int) {
        val smoothScroller = LinearTopSmoothScroller(requireContext(), false)
        smoothScroller.targetPosition = position
        binding.rvFile?.layoutManager?.startSmoothScroll(smoothScroller)
    }

    /**
     * 计算广告的最佳插入位置
     */
    private fun calculateAdPosition(): Int {
        val fileCount = fileAdapter.items.filterIsInstance<BusinessFileInfo>().size
        return calculateAdPosition(fileCount)
    }

    /**
     * 根据文件数量计算广告的最佳插入位置
     */
    private fun calculateAdPosition(fileCount: Int): Int {
        return when {
            fileCount <= 2 -> fileCount  // 文件太少，插入到末尾
            fileCount <= 5 -> 2          // 少量文件，插入到第3个位置
            else -> 3                    // 较多文件，插入到第4个位置
        }
    }

    override fun onResume() {
        super.onResume()
        // 在多选模式下，如果已有数据则不刷新，避免丢失选中状态
        if (config.isChooseMode && fileAdapter.items.isNotEmpty()) {
            return
        }
        refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        LogUtils.d("onHiddenChanged $hidden")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止监听刷新事件
        if (::refreshManager.isInitialized) {
            refreshManager.stopObserving()
        }
    }

}
