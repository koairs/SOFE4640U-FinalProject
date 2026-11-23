package com.example.sofe4640ufinalproject

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout

object ColorBlindHelper {

    private const val PREF_NAME = "app_preferences"
    private const val KEY_COLOR_BLIND = "color_blind_mode"

    // Standard Palette
    val COLOR_PRIMARY_STD = Color.parseColor("#0F2A43")
    val COLOR_ACCENT_STD = Color.parseColor("#14B8A6")
    val COLOR_ACCENT_DARK_STD = Color.parseColor("#0D9488")
    val COLOR_ACCENT_LIGHT_STD = Color.parseColor("#5EEAD4")
    val COLOR_BG_STD = Color.parseColor("#F6F8FB")
    val COLOR_TEXT_PRI_STD = Color.parseColor("#0F172A")
    val COLOR_TEXT_SEC_STD = Color.parseColor("#64748B")
    val COLOR_NAV_INACTIVE_STD = Color.parseColor("#9FB3C8")

    // High-Contrast Color-Blind Safe Palette
    val COLOR_PRIMARY_CB = Color.parseColor("#0B192C")      // Deep Navy
    val COLOR_ACCENT_CB = Color.parseColor("#D97706")       // High-Contrast Amber Gold
    val COLOR_ACCENT_DARK_CB = Color.parseColor("#B45309")  // Deep Amber
    val COLOR_ACCENT_LIGHT_CB = Color.parseColor("#FBBF24") // Bright Gold
    val COLOR_BG_CB = Color.parseColor("#F8FAFC")           // Clean Light Background
    val COLOR_TEXT_PRI_CB = Color.parseColor("#020617")     // High Contrast Black
    val COLOR_TEXT_SEC_CB = Color.parseColor("#334155")     // High Contrast Slate
    val COLOR_NAV_INACTIVE_CB = Color.parseColor("#64748B") // Clear Mid Grey

    fun isColorBlindMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COLOR_BLIND, false)
    }

    fun setColorBlindMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COLOR_BLIND, enabled).apply()
    }

    fun apply(activity: Activity) {
        val isCB = isColorBlindMode(activity)
        val root = activity.findViewById<View>(android.R.id.content)
        root?.post {
            recolorViewTree(root, isCB)
        }
    }

    fun recolorViewTree(view: View, isCB: Boolean) {
        val primary = if (isCB) COLOR_PRIMARY_CB else COLOR_PRIMARY_STD
        val accent = if (isCB) COLOR_ACCENT_CB else COLOR_ACCENT_STD
        val accentDark = if (isCB) COLOR_ACCENT_DARK_CB else COLOR_ACCENT_DARK_STD
        val accentLight = if (isCB) COLOR_ACCENT_LIGHT_CB else COLOR_ACCENT_LIGHT_STD
        val textPri = if (isCB) COLOR_TEXT_PRI_CB else COLOR_TEXT_PRI_STD
        val textSec = if (isCB) COLOR_TEXT_SEC_CB else COLOR_TEXT_SEC_STD
        val navInactive = if (isCB) COLOR_NAV_INACTIVE_CB else COLOR_NAV_INACTIVE_STD
        val bg = if (isCB) COLOR_BG_CB else COLOR_BG_STD

        val background = view.background
        if (background is ColorDrawable) {
            val color = background.color
            if (color == COLOR_PRIMARY_STD || color == COLOR_PRIMARY_CB) {
                view.setBackgroundColor(primary)
            } else if (color == COLOR_BG_STD || color == COLOR_BG_CB) {
                view.setBackgroundColor(bg)
            }
        }

        if (view is MaterialButton) {
            val currentText = view.currentTextColor
            if (currentText == COLOR_ACCENT_LIGHT_STD || currentText == COLOR_ACCENT_LIGHT_CB) {
                view.setTextColor(accentLight)
                view.iconTint = ColorStateList.valueOf(accentLight)
            } else if (currentText == COLOR_ACCENT_DARK_STD || currentText == COLOR_ACCENT_DARK_CB) {
                view.setTextColor(accentDark)
                view.iconTint = ColorStateList.valueOf(accentDark)
                view.strokeColor = ColorStateList.valueOf(accent)
            } else if (currentText == COLOR_NAV_INACTIVE_STD || currentText == COLOR_NAV_INACTIVE_CB) {
                view.setTextColor(navInactive)
                view.iconTint = ColorStateList.valueOf(navInactive)
            } else if (view.backgroundTintList?.defaultColor == COLOR_PRIMARY_STD || view.backgroundTintList?.defaultColor == COLOR_PRIMARY_CB) {
                view.backgroundTintList = ColorStateList.valueOf(primary)
            }
        } else if (view is TextView) {
            val textColor = view.currentTextColor
            if (view.id == R.id.tvPrice || view.id == R.id.tvDetailPrice || view.id == R.id.tvItemTag || view.id == R.id.tvSender) {
                view.setTextColor(accentDark)
            } else if (view.id == R.id.tvItemTitleTag) {
                view.setTextColor(accentLight)
            } else if (textColor == COLOR_PRIMARY_STD || textColor == COLOR_PRIMARY_CB) {
                view.setTextColor(primary)
            } else if (textColor == COLOR_ACCENT_DARK_STD || textColor == COLOR_ACCENT_DARK_CB) {
                view.setTextColor(accentDark)
            } else if (textColor == COLOR_ACCENT_LIGHT_STD || textColor == COLOR_ACCENT_LIGHT_CB) {
                view.setTextColor(accentLight)
            } else if (textColor == COLOR_TEXT_PRI_STD || textColor == COLOR_TEXT_PRI_CB) {
                view.setTextColor(textPri)
            } else if (textColor == COLOR_TEXT_SEC_STD || textColor == COLOR_TEXT_SEC_CB) {
                view.setTextColor(textSec)
            } else if (textColor == COLOR_NAV_INACTIVE_STD || textColor == COLOR_NAV_INACTIVE_CB) {
                view.setTextColor(navInactive)
            }
        }

        if (view is MaterialCardView) {
            val currentStroke = view.strokeColorStateList?.defaultColor ?: 0
            if (currentStroke == COLOR_ACCENT_STD || currentStroke == COLOR_ACCENT_CB || currentStroke == COLOR_ACCENT_DARK_STD || currentStroke == COLOR_ACCENT_DARK_CB) {
                view.strokeColor = accent
            }
        }

        if (view is TextInputLayout) {
            view.boxStrokeColor = accent
            view.setHintTextColor(ColorStateList.valueOf(accentDark))
        }

        if (view is Chip) {
            if (view.isChecked) {
                view.chipBackgroundColor = ColorStateList.valueOf(primary)
                view.setTextColor(Color.WHITE)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                recolorViewTree(view.getChildAt(i), isCB)
            }
        }
    }
}