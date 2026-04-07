package com.documentpro.office.business.fileviewer.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog

abstract class BaseActivity<DB: ViewBinding,VM: BaseModel> : AppCompatActivity(){

    lateinit var binding:DB
    lateinit var model:VM

    var isResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = initBinding()
        enableEdgeToEdge()
        setContentView(binding.root)
        initWindowPadding()
        model = initModel()
        initView()
        initObserve()
    }

    abstract fun initBinding():DB

    abstract fun initModel(): VM

    abstract fun initView()

    abstract fun initObserve()
    abstract fun initTag(): String
    @SuppressLint("RestrictedApi")
    open fun useDefaultToolbar(toolbar: Toolbar, title: String) {
        BarUtils.setStatusBarLightMode(this,true)
        BarUtils.setNavBarVisibility(this,false)
        setSupportActionBar(toolbar)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title
        toolbar.setNavigationOnClickListener {
            closePage()
        }
    }

    open fun initWindowPadding() {
        findViewById<ViewGroup>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BusinessPointLog.logEvent("${initTag()}_Resume",mapOf("tag" to initTag()))
        isResume = true
    }

    override fun onPause() {
        super.onPause()
        BusinessPointLog.logEvent("${initTag()}_Pause",mapOf("tag" to initTag()))
        isResume = false
    }

    override fun onDestroy() {
        super.onDestroy()
        BusinessPointLog.logEvent("${initTag()}_Destroy",mapOf("tag" to initTag()))
    }

    open fun closePage() {
        finish()
    }
}