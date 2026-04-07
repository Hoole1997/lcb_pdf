# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 如果您的项目使用WebView与JS交互，请取消注释以下内容
# 并指定JavaScript接口的完全限定类名：
#-keepclassmembers class com.touka.pdf.fileviewer.ui.web.WebActivity$WebAppInterface {
#   public *;
#}

# 保留行号信息，便于调试
-keepattributes SourceFile,LineNumberTable

# ================== Android 基础组件 ==================
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference

# ================== AndroidX ==================
# 只保留必要的AndroidX类
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.fragment.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.**

# ================== Kotlin ==================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlinx.coroutines.** { *; }
# Kotlin特有规则
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ================== ViewBinding/DataBinding ==================
-keep class **.databinding.*Binding { *; }
-keep class * extends androidx.databinding.ViewDataBinding { *; }
-dontwarn android.databinding.**

# ==================== Kotlin Data Class 混淆规则 ====================

# 保护 Kotlin data class 的构造函数和组件函数
-keepclassmembers class * {
    <init>(...);
}

# 保护 data class 的 componentN 函数
-keepclassmembers class * {
    public ** component*();
}

# 保护 data class 的 copy 函数
-keepclassmembers class * {
    public ** copy(...);
}

# ==================== Gson 混淆规则 ====================

# Gson 使用泛型和注解
-keepattributes Signature
-keepattributes *Annotation*

# Gson 类
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# 保留所有使用 @SerializedName 注解的字段
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留所有使用 @Expose 注解的字段和方法
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.Expose <fields>;
    @com.google.gson.annotations.Expose <methods>;
}

# Gson 类型适配器
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================== UtilCode ==================
-keep class com.blankj.utilcode.util.** { *; }
-dontwarn com.blankj.utilcode.**

# ================== Glide ==================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep class com.bumptech.glide.load.resource.** { *; }
-keep class com.bumptech.glide.integration.** { *; }
-dontwarn com.bumptech.glide.**

# ================== PDF处理 ==================
-keep class com.itextpdf.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.apache.** { *; }
-keep class org.w3c.** { *; }
-keep class org.xml.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.bouncycastle.**

# ================== OkHttp 混淆规则 ==================
# OkHttp - ads-mobile-sdk 和网络请求需要
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# 保留 OkHttp 的内部类（特别是 Util 类）
-keep class okhttp3.internal.** { *; }
-keepclassmembers class okhttp3.internal.** { *; }

# 保留 OkHttp 的注解处理器生成的类
-keepclassmembers class okhttp3.** {
    *;
}

# ================== Google服务 ==================
# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Firebase/FCM
-keep class com.google.firebase.** { *; }
-keep class com.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

# ================== Google Ads Mobile SDK 混淆规则 ==================
# Google Ads Mobile SDK (新的 AdMob SDK) - ads-mobile-sdk 需要保护
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**

# 保留 ads-mobile-sdk 的内部类（混淆后的包名）
-keep class ads_mobile_sdk.** { *; }
-dontwarn ads_mobile_sdk.**

# ================== Facebook SDK ==================
# 核心Facebook SDK
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }

# Facebook 登录
-keep class com.facebook.login.** { *; }
-keep class com.facebook.FacebookActivity { *; }
-keep class com.facebook.CustomTabActivity { *; }

# Facebook 分享
-keep class com.facebook.share.** { *; }

# Facebook 广告
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# Facebook 应用事件
-keep class com.facebook.appevents.** { *; }

# Facebook 图形API
-keep class com.facebook.GraphRequest { *; }
-keep class com.facebook.GraphResponse { *; }

# 保持Facebook注解
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.facebook.internal.method.annotation.* <methods>;
}

# 保持Bolts库 (Facebook SDK依赖)
-keep class bolts.** { *; }
-keep class com.parse.bolts.** { *; }
-dontwarn bolts.**

# Facebook Native库
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================== UI库 ==================
# FlagKit - 国旗图标库（避免资源ID获取失败）
-keep class com.murgupluoglu.flagkit.** { *; }
-dontwarn com.murgupluoglu.flagkit.**
# 保护 FlagKit 的资源获取方法
-keepclassmembers class com.murgupluoglu.flagkit.FlagKit {
    public static int getResId(android.content.Context, java.lang.String);
    public static ** get*(...);
}

# XPopup
-keep class com.lxj.xpopup.core.** { *; }
-keep class com.lxj.xpopup.impl.** { *; }
-dontwarn com.lxj.xpopup.**

# BaseRecyclerViewAdapterHelper4
-keep class com.chad.library.adapter4.** { *; }
-dontwarn com.chad.library.adapter4.**

# 权限
-keep class com.hjq.permissions.** { *; }
-dontwarn com.hjq.permissions.**

# Material
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# TabStrip
-keep class me.majiajie.pagerbottomtabstrip.** { *; }
-dontwarn me.majiajie.pagerbottomtabstrip.**

# SVGA动画
-keep class com.opensource.svgaplayer.** { *; }
-keep class com.opensource.svgaplayer.proto.** { *; }

# ================== 保持自定义实体类 ==================
# 保护所有数据模型类，避免序列化/反序列化问题
-keep class com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo

# ================== 通用保护规则 ==================
# 保留注解
-keepattributes *Annotation*
# 保留泛型信息
-keepattributes Signature
# 保留异常信息
-keepattributes Exceptions
# 保留序列化相关
-keepclassmembers class * implements java.io.Serializable { *; }
# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================== 混淆字典 ==================
-obfuscationdictionary dic.txt
-classobfuscationdictionary dic.txt
-packageobfuscationdictionary dic.txt

# ================== AppLovin SDK ==================
# AppLovin
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
-keepattributes Signature,InnerClasses,Exceptions,*Annotation*
-keep class com.applovin.sdk.AppLovinSdk$Settings { *; }
-keep public class * implements com.applovin.sdk.AppLovinSdkListener
-keep public class * implements com.applovin.adview.AppLovinAdClickListener
-keep public class * implements com.applovin.adview.AppLovinAdDisplayListener
-keep public class * implements com.applovin.adview.AppLovinAdLoadListener
-keep public class * implements com.applovin.adview.AppLovinAdRewardListener
-keep public class * implements com.applovin.sdk.AppLovinEventService
-keep public class * implements com.applovin.mediation.MaxAdListener
-keep public class * implements com.applovin.mediation.MaxRewardedAdListener
-keep public class * implements com.applovin.mediation.MaxAdViewAdListener
-keep public class * implements com.applovin.mediation.MaxAdRevenueListener
-keep public class * implements com.applovin.mediation.MaxAdRequestListener

-keep class com.applovin.adview.** { *; }
-keep class * extends com.applovin.adview.** { *; }
-keepclassmembers class com.applovin.** {
    native <methods>;
    public static final <fields>;
    private static final <fields>;
    public <methods>;
    protected <methods>;
}

# 保护所有数据模型类，避免序列化/反序列化问题
-keep class com.documentpro.office.business.fileviewer.utils.queryfile.** { *; }
#-keep class com.touka.pdf.fileviewer.core.** { *; }

# ================== 广告SDK混淆规则 ==================

# AdMob / Google Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep public class com.google.android.gms.ads.MobileAds { *; }
-keep public class com.google.android.gms.ads.initialization.InitializationStatus { *; }
-keep public class com.google.android.gms.ads.initialization.OnInitializationCompleteListener { *; }
-keep public class com.google.android.gms.ads.AdView { *; }
-keep public class com.google.android.gms.ads.AdLoader { *; }
-keep public class com.google.android.gms.ads.rewarded.** { *; }
-keep public class com.google.android.gms.ads.interstitial.** { *; }
-keep public class com.google.android.gms.ads.nativead.** { *; }
-keep public class com.google.android.gms.ads.admanager.** { *; }

# Facebook
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-keep public class com.facebook.ads.InterstitialAd
-keep public class com.facebook.ads.RewardedVideoAd
-keep public class com.facebook.ads.NativeAd
-keep public class com.facebook.ads.AudienceNetworkAds
-keep public class com.facebook.ads.BuildConfig { *; }

# ironSource
-keepclassmembers class com.ironsource.sdk.controller.IronSourceWebView$JSInterface { public *; }
-keepclassmembers class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
-keep public class com.ironsource.sdk.controller.IronSourceWebView$JSInterface { *; }
-keep public class com.ironsource.sdk.controller.** { *; }
-keep class com.ironsource.** { *; }
-dontwarn com.ironsource.**

# Vungle
-keep class com.vungle.warren.** { *; }
-keep class com.vungle.warren.downloader.DownloadRequest
-dontwarn com.vungle.warren.error.VungleError$ErrorCode
-dontwarn com.vungle.warren.**
-keep class com.vungle.warren.Vungle { public *; }
-keep class com.vungle.warren.InitCallback { public *; }
-keep class com.vungle.warren.LoadAdCallback { public *; }
-keep class com.vungle.warren.PlayAdCallback { public *; }
-keep class com.vungle.warren.AdConfig { public *; }
-keep class com.vungle.warren.BuildConfig { public *; }
-keep class com.vungle.ads.** { *; }
-dontwarn com.vungle.ads.**

# Chartboost
-keep class com.chartboost.** { *; }
-dontwarn com.chartboost.**
-keep class com.chartboost.sdk.** { *; }
-keep class com.chartboost.mediation.** { *; }

# InMobi
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-keep class com.inmobi.monetization.** { *; }
-keep class com.inmobi.omsdk.** { *; }

# Mintegral
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mbridge.** { *; }
-keep class com.mbridge.msdk.** { *; }
-dontwarn com.mbridge.**
-keep class **.R$* { public static final int mbridge*; }

# BidMachine
-keep class io.bidmachine.** { *; }
-dontwarn io.bidmachine.**
-keep class com.explorestack.** { *; }
-dontwarn com.explorestack.**

# Fyber
-keep class com.fyber.** { *; }
-dontwarn com.fyber.**
-keep class com.fyber.marketplace.** { *; }
-keep class com.fyber.omsdk.** { *; }

# Pangle
-keep class com.pangle.** { *; }
-dontwarn com.pangle.**
-keep class com.pangle.global.** { *; }
-keep class com.bytedance.sdk.** { *; }

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**

# Smaato
-keep class com.smaato.** { *; }
-dontwarn com.smaato.**
-keep class com.smaato.android.** { *; }
-keep public class com.smaato.android.sdk.** { *; }

# Verve/HyBid
-keep class net.pubnative.** { *; }
-dontwarn net.pubnative.**
-keep class net.pubnative.hybid.** { *; }

# Anythink
-keep class com.anythink.** { *; }
-dontwarn com.anythink.**
-keep class com.anythink.sdk.** { *; }
-keep class com.anythink.core.** { *; }

# Moloco
-keep class com.moloco.** { *; }
-dontwarn com.moloco.**

# Bigo
-keep class com.bigossp.** { *; }
-dontwarn com.bigossp.**

# Xiaomi
-keep class com.mi.ads.** { *; }
-dontwarn com.mi.ads.**

# TaurusX
-keep class com.taurusx.** { *; }
-dontwarn com.taurusx.**

# Kwai
-keep class io.github.kwainetwork.** { *; }
-dontwarn io.github.kwainetwork.**

# 通用序列化/反序列化规则
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留Parcelable实现类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留自定义的FileInfo类
-keep class com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo { *; }
-keep class com.documentpro.office.business.fileviewer.ui.home.BusinessFileListConfig { *; }
-keep class com.documentpro.office.business.fileviewer.core.scheduled.BusinessNotificationData { *; }
-keep class com.documentpro.office.business.fileviewer.ui.home.BusinessFileListConfig { *; }
-keep class com.documentpro.office.business.fileviewer.ui.pdf.model.BusinessPdfPageInfo { *; }
-keep class com.google.android.gms.** { *; }
# ================== R8 Missing Classes Fix ==================
-dontwarn cn.thinkingdata.ta_apt.TRoute
-dontwarn org.apache.xml.resolver.Catalog
-dontwarn org.apache.xml.resolver.CatalogManager
-dontwarn org.apache.xml.resolver.readers.CatalogReader
-dontwarn org.apache.xml.resolver.readers.SAXCatalogReader

-keep class com.bytedance.sdk.** { *; }
# ================== 通用保护规则 ==================
