package com.documentpro.office.business.fileviewer.ui.web

import android.content.Context
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.activity.viewModels
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityWebBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel

class WebActivity : BaseActivity<ActivityWebBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "WebActivity"
        private const val EXTRA_URL = "extra_url"

        fun launch(context: Context, url: String) {
            val intent = Intent(context, WebActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startActivity(intent)
        }
    }

    override fun initBinding(): ActivityWebBinding {
        return ActivityWebBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun initView() {
        // 设置 Toolbar，默认标题可以为空或加载中
        useDefaultToolbar(binding.toolbar, "")

        // 获取传入的 URL
        val url = intent.getStringExtra(EXTRA_URL)

        // 配置 WebView
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (title != null) {
                    binding.toolbar.title = title
                }
            }
        }

        // 加载 URL
        if (url != null) {
            binding.webView.loadUrl(url)
        } else {
            // 处理 URL 为空的情况，例如显示错误或关闭 Activity
            // Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            finish()
        }
        onBackPressedDispatcher.addCallback {
            execAction_1 ()
        }
    }

    override fun initObserve() {
        // 如果 BusinessMainModel 有需要观察的数据，可以在这里实现
    }

    override fun initTag(): String {
        return TAG
    }


    private fun execAction_1() {
        // 如果 WebView 可以返回，则 WebView 返回，否则 Activity 返回
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }
} 