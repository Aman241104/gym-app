package app.gymapp.domain

import kotlin.math.abs
import kotlin.math.atan2

/** A pose landmark in pixels (MediaPipe gives 0..1, scaled by the photo size before this). */
data class Pt(val x: Float, val y: Float, val visibility: Float)

// MediaPipe Pose landmark indices.
private const val NOSE = 0
private const val L_EAR = 7
private const val R_EAR = 8
private const val L_SHOULDER = 11
private const val R_SHOULDER = 12
private const val L_HIP = 23
private const val R_HIP = 24

sealed interface ScanResult {
    /** Degrees the ear sits in front of the shoulder, and the shoulder in front of the hip, measured from vertical. */
    data class Side(val headForward: Float, val upperBack: Float, val ear: Pt, val shoulder: Pt, val hip: Pt) : ScanResult
    /** Tilt in degrees; positive means the person's left side is lower. */
    data class Front(val shoulderTilt: Float, val hipTilt: Float, val points: List<Pt>) : ScanResult
    data class Unclear(val reason: String) : ScanResult
}

private fun deg(rad: Double) = Math.toDegrees(rad).toFloat()

/**
 * Side photo. Uses whichever side faces the camera more clearly. The shoulder stands in for
 * C7 (MediaPipe has no neck point), so read the numbers as your own trend, not a clinical CVA.
 */
fun analyzeSide(p: List<Pt>): ScanResult {
    if (p.size < 25) return ScanResult.Unclear("No body found. Stand side-on, whole body in frame.")
    val left = listOf(p[L_EAR], p[L_SHOULDER], p[L_HIP])
    val right = listOf(p[R_EAR], p[R_SHOULDER], p[R_HIP])
    val (ear, shoulder, hip) = if (left.sumOf { it.visibility.toDouble() } >= right.sumOf { it.visibility.toDouble() }) left else right
    if (listOf(ear, shoulder, hip).any { it.visibility < 0.5f }) return ScanResult.Unclear("Couldn't see your ear, shoulder and hip clearly. Try a plain background and side-on stance.")
    // Facing direction from where the nose is relative to the ear, so "forward" means forward.
    val facing = if (p[NOSE].x >= ear.x) 1f else -1f
    val headForward = deg(atan2(((ear.x - shoulder.x) * facing).toDouble(), (shoulder.y - ear.y).toDouble()))
    val upperBack = deg(atan2(((shoulder.x - hip.x) * facing).toDouble(), (hip.y - shoulder.y).toDouble()))
    return ScanResult.Side(headForward, upperBack, ear, shoulder, hip)
}

/** Front photo: shoulder and hip level. Image x grows to the right, which is the person's left. */
fun analyzeFront(p: List<Pt>): ScanResult {
    if (p.size < 25) return ScanResult.Unclear("No body found. Face the camera, whole body in frame.")
    val pts = listOf(p[L_SHOULDER], p[R_SHOULDER], p[L_HIP], p[R_HIP])
    if (pts.any { it.visibility < 0.5f }) return ScanResult.Unclear("Couldn't see both shoulders and hips. Face the camera squarely.")
    fun tilt(l: Pt, r: Pt) = deg(atan2((l.y - r.y).toDouble(), abs(l.x - r.x).toDouble()))
    return ScanResult.Front(tilt(p[L_SHOULDER], p[R_SHOULDER]), tilt(p[L_HIP], p[R_HIP]), pts)
}

/** Rough bands for the head-forward angle; the trend against your own first scan matters more. */
fun headForwardLabel(deg: Float): String = when {
    deg < 15 -> "Head is well stacked over the shoulders."
    deg < 25 -> "Mild forward head. Chin tucks and the daily posture block will help."
    else -> "Marked forward head. Keep up chin tucks and check in with a physio if you get neck pain."
}

fun tiltLabel(part: String, deg: Float): String = when {
    abs(deg) < 2 -> "${part.replaceFirstChar(Char::uppercase)}s level."
    deg > 0 -> "Left $part lower by %.1f°.".format(deg)
    else -> "Right $part lower by %.1f°.".format(-deg)
}
