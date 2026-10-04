package com.dd.sms.hook.shared.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dd.sms.hook.R

private val OpenSans: FontFamily = FontFamily(
    Font(R.font.open_sans_regular_400, FontWeight.Normal),
    Font(R.font.open_sans_semi_bold_600, FontWeight.SemiBold),
)

/**
 * The whole app uses 4 sizes and 2 weights so hierarchy comes from size, weight and colour, not variety.
 * - display 28: screen titles and headline numbers
 * - title 18: card and section titles
 * - body 15: content and buttons
 * - caption 13: secondary text and labels
 */
private object TypeScale {
    val display: TextUnit = 28.sp
    val title: TextUnit = 18.sp
    val body: TextUnit = 15.sp
    val caption: TextUnit = 13.sp
    val displayLine: TextUnit = 36.sp
    val titleLine: TextUnit = 24.sp
    val bodyLine: TextUnit = 22.sp
    val captionLine: TextUnit = 18.sp
}

private fun style(size: TextUnit, line: TextUnit, weight: FontWeight): TextStyle =
    TextStyle(fontFamily = OpenSans, fontSize = size, lineHeight = line, fontWeight = weight)

private val Display: TextStyle = style(TypeScale.display, TypeScale.displayLine, FontWeight.SemiBold)
private val Title: TextStyle = style(TypeScale.title, TypeScale.titleLine, FontWeight.SemiBold)
private val BodyStrong: TextStyle = style(TypeScale.body, TypeScale.bodyLine, FontWeight.SemiBold)
private val Body: TextStyle = style(TypeScale.body, TypeScale.bodyLine, FontWeight.Normal)
private val CaptionStrong: TextStyle = style(TypeScale.caption, TypeScale.captionLine, FontWeight.SemiBold)
private val Caption: TextStyle = style(TypeScale.caption, TypeScale.captionLine, FontWeight.Normal)

internal val AppTypography: Typography = Typography(
    displayLarge = Display,
    displayMedium = Display,
    displaySmall = Display,
    headlineLarge = Display,
    headlineMedium = Display,
    headlineSmall = Display,
    titleLarge = Title,
    titleMedium = Title,
    titleSmall = BodyStrong,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = BodyStrong,
    labelMedium = CaptionStrong,
    labelSmall = Caption,
)

/** For request/response bodies and URLs. */
val CodeFontFamily: FontFamily = FontFamily.Monospace

/** Tabular digits so numbers in stats and lists line up and do not jiggle when they change. */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
