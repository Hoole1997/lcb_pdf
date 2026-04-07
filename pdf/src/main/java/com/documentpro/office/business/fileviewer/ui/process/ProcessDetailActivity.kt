package com.documentpro.office.business.fileviewer.ui.process

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityProcessDetailBinding
import com.documentpro.office.business.fileviewer.ui.success.BusinessSuccessModel
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.RandomInterstitialController
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ProcessDetailActivity : BaseActivity<ActivityProcessDetailBinding, BusinessSuccessModel>() {

    companion object {
        private const val TAG = "ProcessDetailActivity"

        fun start(context: FragmentActivity) {
            val intent = Intent(context, ProcessDetailActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var processAdapter: ProcessListAdapter
    private val processList = mutableListOf<ProcessInfo>()

    override fun initBinding(): ActivityProcessDetailBinding {
        return ActivityProcessDetailBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessSuccessModel {
        return viewModels<BusinessSuccessModel>().value
    }

    override fun finish() {
        RandomInterstitialController.showRandomInterstitial(this, onAdDismissed = {
            super.finish()
        })
    }

    override fun initView() {

        useDefaultToolbar(binding.toolbar, "")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        // 初始化适配器
        processAdapter = ProcessListAdapter { processInfo ->
            BusinessSplashForegroundController.markNextIntercept()
            stopProcess(processInfo)
        }
        binding.rvProcesses.adapter = processAdapter

        // 加载进程列表
        loadRunningProcesses()

        // Done 按钮点击事件
        binding.btnDone.setOnClickListener {
            startActivity(Intent(this,ProcessSuccessActivity::class.java).apply {
                putExtra("processCount",processAdapter.processList.filter { it.isStopped }.size)
            })
            finish()
        }

        // 加载底部Banner广告
        loadNative(binding.adContainer)
    }

    override fun initObserve() {
    }

    override fun initTag(): String {
        return TAG
    }

    /**
     * 加载运行中的进程
     */
    private fun loadRunningProcesses() {
        lifecycleScope.launch {
            val processes = withContext(Dispatchers.IO) {
                getRunningProcesses()
            }

            processList.clear()
            processList.addAll(processes)

            // 更新内存信息
            updateMemoryInfo()

            // 提交列表
            processAdapter.submitList(processes)

            // 等待一帧后启动动画，确保列表已渲染
            binding.rvProcesses.post {
                startListWaveAnimation()
            }
        }
    }

    /**
     * 启动列表波浪动画
     */
    private fun startListWaveAnimation() {
        lifecycleScope.launch {
            for (i in 0 until processList.size) {
                // 获取对应的 ViewHolder
                val viewHolder = binding.rvProcesses.findViewHolderForAdapterPosition(i)
                viewHolder?.itemView?.let { itemView ->
                    // 设置初始状态
                    itemView.alpha = 0f
                    itemView.translationY = 50f

                    // 启动动画
                    itemView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }

                // 每个item之间延迟80ms，形成波浪效果
                kotlinx.coroutines.delay(80)
            }
        }
    }

    /**
     * 获取运行中的进程列表
     */
    private fun getRunningProcesses(): List<ProcessInfo> {
        val pm = packageManager
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return emptyList()

        val processInfoList = mutableListOf<ProcessInfo>()

        // 获取所有已安装的应用
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val userApps = installedApps.filter { 
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName
        }

        for (appInfo in userApps.take(10)) { // 限制显示前10个应用
            try {
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo.packageName)
                
                processInfoList.add(
                    ProcessInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = icon,
                        memoryUsage = 0L,
                        isStopped = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return processInfoList
    }

    /**
     * 更新内存信息显示（带动画）
     */
    private fun updateMemoryInfo() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val usedMemory = totalMemory - availableMemory

        // 计算百分比
        val targetPercent = (usedMemory.toFloat() / totalMemory.toFloat() * 100).toInt()

        // 更新内存详情文字
        binding.tvMemoryDetail.text = "${formatMemorySize(usedMemory)}/${formatMemorySize(totalMemory)}"

        // 百分比数字滚动动画
        animatePercentage(targetPercent)

        // 进度条宽度动画
        animateProgressBar(targetPercent)
    }

    /**
     * 百分比数字滚动动画
     */
    private fun animatePercentage(targetPercent: Int) {
        ValueAnimator.ofInt(0, targetPercent).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                binding.tvMemoryPercent.text = "$value%"
            }
            start()
        }
    }

    /**
     * 进度条宽度动画
     */
    private fun animateProgressBar(targetPercent: Int) {
        // 等待布局完成后再执行动画
        binding.progressForeground.post {
            val progressWidth = binding.root.width - 50 * 2 // 减去左右margin
            val targetWidth = (progressWidth * targetPercent / 100f).toInt()

            ValueAnimator.ofInt(0, targetWidth).apply {
                duration = 1000
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val width = animation.animatedValue as Int
                    val params = binding.progressForeground.layoutParams
                    params.width = width
                    binding.progressForeground.layoutParams = params
                }
                start()
            }
        }
    }

    /**
     * 格式化内存大小
     */
    private fun formatMemorySize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        val df = DecimalFormat("#.#")
        return "${df.format(gb)}GB"
    }

    /**
     * 停止进程 - 跳转到应用系统设置页
     */
    private fun stopProcess(processInfo: ProcessInfo) {
        try {
            // 创建跳转到应用详情页的 Intent
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", processInfo.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            
            // 标记为已停止（用户可能会去停止）
            val position = processList.indexOf(processInfo)
            if (position != -1) {
                processInfo.isStopped = true
                processAdapter.updateItem(position)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果跳转失败，尝试打开通用设置页
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun initWindowPadding() {
        findViewById<ViewGroup>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

