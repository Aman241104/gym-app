package app.gymapp.domain

/** The full checklist on the You tab. The planner will build plans only from what's ticked here. */
val EquipmentGroups: List<Pair<String, List<String>>> = listOf(
    "Free weights" to listOf("Dumbbells", "Adjustable dumbbells", "Barbell", "EZ bar", "Weight plates", "Kettlebells", "Trap bar"),
    "Benches & racks" to listOf("Flat bench", "Adjustable bench", "Squat / power rack", "Smith machine", "Pull-up bar", "Dip station"),
    "Machines" to listOf(
        "Seated row machine", "Lat pulldown", "Cable machine", "Leg press", "Leg extension", "Leg curl",
        "Chest press machine", "Pec deck", "Shoulder press machine", "Hack squat", "Calf raise machine", "Assisted pull-up machine",
    ),
    "Cardio" to listOf("Treadmill", "Rowing machine", "Stationary bike", "Elliptical", "Jump rope"),
    "Mobility & extras" to listOf(
        "Resistance bands", "Mini bands", "Foam roller", "Yoga mat", "Lacrosse ball", "Stretch strap",
        "Ab wheel", "Stability ball", "Medicine ball", "Suspension trainer",
    ),
)
