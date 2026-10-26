package app.gymapp.domain

/** One timed step of a guided routine. Two-sided moves become two steps ("Left side", "Right side"). */
data class Move(val name: String, val seconds: Int, val cues: List<String>, val why: String, val side: String? = null)

private fun bothSides(name: String, seconds: Int, cues: List<String>, why: String) =
    listOf(Move(name, seconds, cues, why, "Left side"), Move(name, seconds, cues, why, "Right side"))

/**
 * Daily ~10-minute block for forward head and rounded shoulders: open the tight front
 * (chest, upper traps, neck), mobilise the upper back, then wake up the weak back (deep neck
 * flexors, mid/lower traps). Swaps moves based on the equipment you ticked.
 */
fun postureRoutine(equipment: List<String>): List<Move> {
    val has = equipment.toSet()
    val tSpine = if ("Foam roller" in has) Move(
        "Foam roller T-spine extension", 60,
        listOf(
            "Lie back with the roller across your upper back, below the shoulder blades.",
            "Hands behind your head to support your neck, hips on the floor.",
            "Gently arch back over the roller, breathe out, come back up.",
            "Move the roller up an inch and repeat. Stay off the lower back.",
        ),
        "Rounded shoulders usually come with a stiff upper back. This restores extension there.",
    ) else Move(
        "Chair T-spine extension", 60,
        listOf(
            "Sit in a chair with a backrest that reaches your shoulder blades.",
            "Hands behind your head, elbows wide.",
            "Lean back over the top of the backrest and open your chest, then return.",
            "Slow and smooth, breathe out as you extend.",
        ),
        "Rounded shoulders usually come with a stiff upper back. This restores extension there.",
    )
    val pull = when {
        "Resistance bands" in has -> Move(
            "Band pull-aparts", 60,
            listOf(
                "Hold a band at shoulder height, arms straight, hands shoulder-width.",
                "Pull the band apart until it touches your chest, squeezing your shoulder blades together.",
                "Control it back. Keep your ribs down and neck long.",
                "About 15 slow reps, rest, repeat.",
            ),
            "Strengthens the mid back and rear delts that pull your shoulders back.",
        )
        "Cable machine" in has -> Move(
            "Face pulls (light cable)", 60,
            listOf(
                "Rope at upper-chest height, light weight.",
                "Pull the rope toward your eyes, elbows high, hands ending beside your ears.",
                "Pause and squeeze the back of the shoulders, then return slowly.",
                "About 15 reps, rest, repeat.",
            ),
            "Strengthens the mid back and rear delts that pull your shoulders back.",
        )
        else -> Move(
            "Prone T raises", 60,
            listOf(
                "Lie face down, arms out to the sides in a T, thumbs up.",
                "Squeeze your shoulder blades together and lift your arms a few centimetres.",
                "Hold 2 seconds, lower. Keep your forehead down and neck long.",
            ),
            "Strengthens the mid back and rear delts that pull your shoulders back.",
        )
    }

    return buildList {
        add(Move(
            "Chin tucks", 60,
            listOf(
                "Sit or stand tall, eyes level.",
                "Glide your head straight back, as if making a double chin. Don't nod down.",
                "Hold 3 seconds, release. Repeat slowly.",
            ),
            "Retrains the deep neck muscles that hold your head over your shoulders.",
        ))
        addAll(bothSides(
            "Upper trap stretch", 30,
            listOf(
                "Sit tall and hold the chair seat with one hand.",
                "Tilt your ear toward the opposite shoulder.",
                "Rest your free hand on your head for light extra pull. Breathe slowly.",
            ),
            "Forward head posture keeps these muscles short and tight.",
        ))
        addAll(bothSides(
            "Levator scapulae stretch", 30,
            listOf(
                "Turn your nose toward your armpit and look down.",
                "Gently pull your head down with the hand on that side.",
                "Feel it at the back of the neck, toward the shoulder blade.",
            ),
            "Releases the muscle that hikes your shoulder blade up toward your ear.",
        ))
        add(Move(
            "Doorway chest stretch", 60,
            listOf(
                "Forearms on the door frame, elbows at shoulder height.",
                "Step one foot through and lean gently forward.",
                "Keep your chin tucked and ribs down. Stretch, don't strain.",
            ),
            "Tight chest muscles pull your shoulders forward.",
        ))
        add(tSpine)
        add(Move(
            "Wall angels", 60,
            listOf(
                "Back, head and hips against a wall, feet a small step out.",
                "Arms in a goalpost shape, backs of the hands toward the wall.",
                "Slide your arms up slowly, then down. Keep your ribs down.",
                "Only go as high as you can keep contact.",
            ),
            "Teaches your shoulder blades to move back and down instead of forward.",
        ))
        add(pull)
        add(Move(
            "Prone Y raises", 45,
            listOf(
                "Lie face down, arms overhead in a Y, thumbs up.",
                "Draw your shoulder blades down and back, lift your arms a little.",
                "Hold 2 seconds, lower. Neck long, chin tucked.",
            ),
            "Wakes up the lower traps, the main muscle for shoulders-back posture.",
        ))
        addAll(bothSides(
            "Thread the needle", 30,
            listOf(
                "On hands and knees, slide one arm under your body, palm up.",
                "Let that shoulder and the side of your head rest on the floor.",
                "Breathe into the upper back, then reach the arm up to the ceiling to finish.",
            ),
            "Rotation for the upper back, which sitting and phone use lock up.",
        ))
        add(Move(
            "Child's pose reach", 45,
            listOf(
                "Kneel, sit back on your heels and reach both arms forward on the floor.",
                "Let your chest sink toward the floor.",
                "Slow breaths. Walk your hands to one side, then the other.",
            ),
            "Opens the lats and finishes the routine calm.",
        ))
    }
}

/** After-run cooldown: calves, hamstrings, hip flexors, quads and glutes, ~6 minutes. */
fun runnerStretches(): List<Move> = buildList {
    addAll(bothSides("Calf stretch on a wall", 30, listOf(
        "Hands on a wall, one foot back, heel down, back knee straight.",
        "Lean in until you feel the calf. Then bend the back knee slightly for the lower calf.",
    ), "Running loads the calves more than anything else."))
    addAll(bothSides("Standing hamstring stretch", 30, listOf(
        "Heel on a low step or kerb, leg straight, toes up.",
        "Hinge forward from the hips with a flat back until you feel the back of the thigh.",
    ), "Keeps the hamstrings from tightening up after runs."))
    addAll(bothSides("Half-kneeling hip flexor stretch", 30, listOf(
        "Kneel on one knee, other foot in front.",
        "Squeeze the glute of the kneeling side and shift your hips forward. Keep your ribs down.",
    ), "Hip flexors shorten with running and sitting; this also helps posture."))
    addAll(bothSides("Standing quad stretch", 30, listOf(
        "Hold a wall, pull one heel toward your glute.",
        "Knees together, hips pushed slightly forward.",
    ), "Releases the front of the thigh after striding."))
    addAll(bothSides("Figure-4 glute stretch", 30, listOf(
        "Lie on your back, ankle over the opposite knee.",
        "Pull the bottom thigh toward you until you feel the outer hip.",
    ), "Glutes and outer hips work hard to keep your pelvis level."))
}
