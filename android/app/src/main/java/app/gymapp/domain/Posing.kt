package app.gymapp.domain

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

// MediaPipe indices used by the posing coach.
private const val L_SHOULDER = 11
private const val R_SHOULDER = 12
private const val L_ELBOW = 13
private const val R_ELBOW = 14
private const val L_WRIST = 15
private const val R_WRIST = 16
private const val L_HIP = 23
private const val R_HIP = 24

/** Interior angle at [b] in degrees (0-180). */
fun angleAt(a: Pt, b: Pt, c: Pt): Float {
    val (x1, y1) = a.x - b.x to a.y - b.y
    val (x2, y2) = c.x - b.x to c.y - b.y
    val d = sqrt(x1 * x1 + y1 * y1) * sqrt(x2 * x2 + y2 * y2)
    if (d == 0f) return 0f
    return Math.toDegrees(acos(((x1 * x2 + y1 * y2) / d).coerceIn(-1f, 1f).toDouble())).toFloat()
}

enum class Side(val label: String) { Left("left"), Right("right") }
enum class Joint { Elbow, Shoulder }

/** What one joint should look like: [target] ± [tolerance] degrees. */
data class AngleTarget(val side: Side, val joint: Joint, val target: Float, val tolerance: Float)

data class Pose(val name: String, val how: String, val targets: List<AngleTarget>)

private fun both(joint: Joint, target: Float, tol: Float) = listOf(AngleTarget(Side.Left, joint, target, tol), AngleTarget(Side.Right, joint, target, tol))

/**
 * Elbow = upper arm vs forearm (180 = straight). Shoulder = torso vs upper arm (0 = arm by your
 * side, 90 = arm out level with the shoulder). Simple joint maths, so read scores as coaching.
 */
val Poses: List<Pose> = listOf(
    Pose("Front relaxed", "Stand tall, arms slightly away from your sides, lats wide, chest up.", both(Joint.Shoulder, 20f, 10f) + both(Joint.Elbow, 165f, 15f)),
    Pose("Front double biceps", "Upper arms level with your shoulders, fists up, squeeze the biceps.", both(Joint.Shoulder, 90f, 12f) + both(Joint.Elbow, 70f, 15f)),
    Pose("Back double biceps", "Same arms as the front pose, facing away. Spread the back.", both(Joint.Shoulder, 90f, 12f) + both(Joint.Elbow, 70f, 15f)),
    Pose("Front lat spread", "Fists on your waist, elbows out and forward, flare the lats.", both(Joint.Shoulder, 50f, 12f) + both(Joint.Elbow, 95f, 18f)),
    Pose("Casual", "One hand in a pocket or on the hip, the other arm loose. Shoulders back.", listOf(AngleTarget(Side.Left, Joint.Elbow, 110f, 35f), AngleTarget(Side.Right, Joint.Elbow, 165f, 20f))),
)

data class JointReading(val target: AngleTarget, val actual: Float) {
    val error: Float get() = actual - target.target
    val ok: Boolean get() = abs(error) <= target.tolerance
}

data class PoseCheck(val score: Int, val readings: List<JointReading>, val tip: String)

fun checkPose(p: List<Pt>, pose: Pose): PoseCheck? {
    if (p.size < 25) return null
    val needed = listOf(L_SHOULDER, R_SHOULDER, L_ELBOW, R_ELBOW, L_WRIST, R_WRIST, L_HIP, R_HIP)
    if (needed.any { p[it].visibility < 0.4f }) return null
    fun read(t: AngleTarget): Float {
        val (sh, el, wr, hip) = if (t.side == Side.Left) listOf(p[L_SHOULDER], p[L_ELBOW], p[L_WRIST], p[L_HIP]) else listOf(p[R_SHOULDER], p[R_ELBOW], p[R_WRIST], p[R_HIP])
        return if (t.joint == Joint.Elbow) angleAt(sh, el, wr) else angleAt(hip, sh, el)
    }
    val readings = pose.targets.map { JointReading(it, read(it)) }
    // Each joint scores 1 inside its tolerance, fading to 0 at 2.5x the tolerance.
    val score = readings.map { r -> (1f - ((abs(r.error) - r.target.tolerance).coerceAtLeast(0f) / (r.target.tolerance * 1.5f))).coerceIn(0f, 1f) }.average()
    val worst = readings.filterNot { it.ok }.maxByOrNull { abs(it.error) / it.target.tolerance }
    val tip = when {
        worst == null -> "Hold it. Breathe and squeeze."
        worst.target.joint == Joint.Shoulder && worst.error < 0 -> "Raise your ${worst.target.side.label} elbow"
        worst.target.joint == Joint.Shoulder -> "Lower your ${worst.target.side.label} elbow"
        worst.error > 0 -> "Bend your ${worst.target.side.label} arm more"
        else -> "Open your ${worst.target.side.label} arm a little"
    }
    return PoseCheck((score * 100).toInt(), readings, tip)
}
