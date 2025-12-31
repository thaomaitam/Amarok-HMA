package deltazero.amarok.xposed

import com.github.kyuubiran.ezxhelper.Log

// Logging extension functions for consistent XHide logging
fun logD(tag: String, msg: String) = Log.d("[$tag] $msg", null)
fun logI(tag: String, msg: String) = Log.ix("[$tag] $msg", null)
fun logW(tag: String, msg: String, e: Throwable? = null) = Log.wx("[$tag] $msg", e)
fun logE(tag: String, msg: String, e: Throwable? = null) = Log.ex("[$tag] $msg", e)
