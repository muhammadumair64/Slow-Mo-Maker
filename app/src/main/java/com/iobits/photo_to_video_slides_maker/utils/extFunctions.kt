package com.iobits.photo_to_video_slides_maker.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.iobits.photo_to_video_slides_maker.BuildConfig
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch






fun Activity.changeStatusBarColor(activity : Activity , colorId : Int){
    val window: Window = activity.window
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = ContextCompat.getColor(activity,colorId)
}


fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}
fun ByteArray.toBitmap(): Bitmap? {
    return BitmapFactory.decodeByteArray(this, 0, size)
}
fun View.gone() {
    visibility = View.GONE
}
fun EditText.onDone(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke()
            return@setOnEditorActionListener true
        }
        false
    }
}
fun View.animateView() {
    val scaleX: ObjectAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0.9f, 1.1f)
    val scaleY: ObjectAnimator = ObjectAnimator.ofFloat(this, "scaleY", 0.9f, 1.1f)
    scaleX.repeatCount = ObjectAnimator.INFINITE
    scaleX.repeatMode = ObjectAnimator.REVERSE
    scaleY.repeatCount = ObjectAnimator.INFINITE
    scaleY.repeatMode = ObjectAnimator.REVERSE
    val scaleAnim = AnimatorSet()
    scaleAnim.duration = 1000
    scaleAnim.play(scaleX).with(scaleY)
    scaleAnim.start()

}

fun Fragment.disableMultipleClicking(view: View, delay: Long = 750) {
    view.isEnabled = false
    this.lifecycleScope.launch {
        delay(delay)
        view.isEnabled = true
    }
}
fun AppCompatActivity.disableMultipleClicking(view: View, delay: Long = 750) {
    view.isEnabled = false
    this.lifecycleScope.launch {
        delay(delay)
        view.isEnabled = true
    }
}

fun Fragment.handleBackPress(onBackPressed: () -> Unit) {
    var lastBackPressedTime = 0L  // Variable to store the last back button press time

    requireView().isFocusableInTouchMode = true
    requireView().requestFocus()
    requireView().setOnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressedTime > 1000) {  // Check if more than 2 seconds have passed
                lastBackPressedTime = currentTime
                onBackPressed() // Call the provided callback function
            }
            true
        } else false
    }
}
fun Fragment.navigateTo(actionId: Int, destinationName: Int) {
    findNavController().navigate(
        actionId, null, NavOptions.Builder().setPopUpTo(destinationName, true).build()
    )
}
fun Fragment.clearBackStack(destinationId: Int, inclusive: Boolean = false) {
    try {
        findNavController().popBackStack(destinationId, inclusive)
    } catch (e: IllegalArgumentException) {
        Log.e("CLEAR_BACKSTACK_ERROR", "Error clearing back stack: ${e.localizedMessage}")
    }
}
fun Fragment.handleLastBackPress(func:() -> Unit){
    // This callback will only be called when MyFragment is at least Started.
    val callback: OnBackPressedCallback =
        object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                func.invoke()
            }
        }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
}

fun Fragment.popBackStack() {
    try {
        findNavController().navigateUp()
    } catch (e: IllegalArgumentException) {
        Log.e("CLEAR_BACKSTACK_ERROR", "Error clearing back stack: ${e.localizedMessage}")
    }
}

fun Fragment.safeNavigate(actionId: Int, currentFragmentId: Int) {
    try{
        if (findNavController().currentDestination?.id == currentFragmentId) {
            findNavController().navigate(
                actionId
            )
        } else {
            Log.d("TAG", "navigateSafe: ")
        }
    }catch (e:Exception){
        Log.d("SAFE_NAV_ERROR", "safeNavigateError:${e.localizedMessage} ")
    }
}
fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
fun Fragment.showToast(string: String) {
    Toast.makeText(this.requireContext(), string, Toast.LENGTH_SHORT).show()
}
fun Fragment.showLongToast(string: String) {
    Toast.makeText(this.requireContext(), string, Toast.LENGTH_LONG).apply {
        cancel() // Cancel any previous toast
        show()
    }
}
fun Fragment.showKeyboard(view: View?) {
    view?.let {
        val imm = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(it, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }
}
fun Fragment.hideKeyboard(view: View?): Boolean {
    val inputMethodManager =
        view?.context?.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager
    return inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0) ?: false
}

fun Context.showEmailChooser(supportEmail: String, subject: String,body: String?=null){
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        val chooser = Intent.createChooser(intent, "Send Email")
        if (chooser.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
    }
}


fun Fragment.showSettingsDialog() {
    val dialog = Dialog(requireContext())
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(false)
    dialog.setContentView(com.iobits.photo_to_video_slides_maker.R.layout.setting_dialogue)

    val width = (requireContext().resources.displayMetrics.widthPixels * 0.90).toInt()
    dialog.setCancelable(false)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

    dialog.findViewById<TextView>(com.iobits.photo_to_video_slides_maker.R.id.yes).setOnClickListener {
        openAppSettingsStorage()
        dialog.dismiss()
    }

    dialog.findViewById<ImageView>(com.iobits.photo_to_video_slides_maker.R.id.closeBtn).setOnClickListener {
        dialog.dismiss()
    }

    try {
        dialog.show()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

private fun Fragment.openAppSettingsStorage() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", requireContext().packageName, null)
    intent.data = uri
    requireContext().startActivity(intent)
}
fun Fragment.showRatingDialog() {
    val dialog = Dialog(requireContext())
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(true)
    dialog.setContentView(R.layout.layout_rate_us)
    val width = (requireContext().resources.displayMetrics.widthPixels * 0.90).toInt()
    dialog.setCancelable(true)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
    dialog.findViewById<RelativeLayout>(R.id.OK).setOnClickListener {
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.IS_SHOW_RATE_US,false)
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
        )
        startActivity(intent)
        dialog.dismiss()
    }

    try {
        dialog.show()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

fun Fragment.watchAdOrBuyPremium(
    context: Activity,
    onCloseClick: () -> Unit = {},
    onBuyPremium: () -> Unit,
    onWatchAd: () -> Unit
) {
    val dialogBuilder = AlertDialog.Builder(context)
    val dialogView = context?.layoutInflater?.inflate(R.layout.layout_buy_pro_watch_ad, null)
    val buyPremium = dialogView?.findViewById<RelativeLayout>(R.id.buyPremium)
    val watchAd = dialogView?.findViewById<RelativeLayout>(R.id.watchAdAndContinue)
    val closebtn = dialogView?.findViewById<ImageView>(R.id.closeBtn)
    dialogBuilder.setView(dialogView)
    val alertDialog = dialogBuilder.create()
    alertDialog.show()
    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    closebtn?.setOnClickListener {
        alertDialog.dismiss()
        onCloseClick.invoke()
    }
    buyPremium?.setOnClickListener {
        alertDialog.dismiss()
        onBuyPremium.invoke()
    }
    watchAd?.setOnClickListener {
        alertDialog.dismiss()
        onWatchAd.invoke()
    }
}

 fun Fragment.showExitDialogue(context: Activity, onYesClick: () -> Unit = {}) {
    // true the preference
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(false)
    dialog.setContentView(R.layout.exit_dialogue)
    val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
    dialog.setCancelable(false)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

    dialog.findViewById<ImageView>(R.id.cross).setOnClickListener {
        dialog.dismiss()
    }
    dialog.findViewById<TextView>(R.id.no).setOnClickListener {
        dialog.dismiss()

    }
    dialog.findViewById<CardView>(R.id.yes).setOnClickListener {
        dialog.dismiss()
        onYesClick.invoke()
    }
    try {
        dialog.show()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}