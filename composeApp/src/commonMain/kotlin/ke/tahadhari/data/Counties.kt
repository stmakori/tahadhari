package ke.tahadhari.data

/**
 * All 47 counties with approximate headquarters coordinates.
 * Coordinates are approximate (good enough for 9 km weather grid cells).
 *
 * Landslide-prone counties ([County.landslideProne]) are marked by passing `landslideAreas` (an empty
 * list = prone, no specific areas named). Each has a source comment. Only counties with a
 * reported landslide event or a published susceptibility study are marked; others are left
 * false rather than guessed (unverified: Kisii, Nyamira, Kirinyaga, Nyandarua, Embu, Meru,
 * Tharaka-Nithi, Vihiga, Kakamega, Nakuru; see the project report).
 * TODO(week 2): verify coordinates, river points and hotspots against official sources.
 */
object Counties {
    private fun c(
        code: Int, name: String, town: String, lat: Double, lon: Double,
        river: String? = null, riverLat: Double? = null, riverLon: Double? = null,
        hotspots: List<String> = emptyList(),
        landslideAreas: List<String>? = null,
    ) = County(
        code = code, name = name, town = town, location = GeoPoint(lat, lon),
        riverName = river,
        riverPoint = if (riverLat != null && riverLon != null) GeoPoint(riverLat, riverLon) else null,
        hotspots = hotspots,
        landslideProne = landslideAreas != null,
        landslideAreas = landslideAreas.orEmpty(),
    )

    val all: List<County> = listOf(
        c(1, "Mombasa", "Mombasa", -4.0435, 39.6682),
        c(2, "Kwale", "Kwale", -4.1737, 39.4521),
        c(3, "Kilifi", "Kilifi", -3.6305, 39.8499),
        c(4, "Tana River", "Hola", -1.4990, 40.0300, "Tana River", -2.2706, 40.1167, listOf("Garsen", "Tana Delta")),
        c(5, "Lamu", "Lamu", -2.2717, 40.9020),
        // Landslide source: Taita Hills: Kenya News Agency and People Daily reports of fatal landslides at Mwatate (Kighombo, Mwasere) and Wundanyi (Mwakale)
        c(6, "Taita-Taveta", "Wundanyi", -3.3961, 38.3569, landslideAreas = listOf("Mwatate", "Wundanyi")),
        c(7, "Garissa", "Garissa", -0.4532, 39.6461, "Tana River", -0.4532, 39.6461, listOf("Garissa town riverside")),
        c(8, "Wajir", "Wajir", 1.7471, 40.0573),
        c(9, "Mandera", "Mandera", 3.9373, 41.8569),
        c(10, "Marsabit", "Marsabit", 2.3346, 37.9899),
        c(11, "Isiolo", "Isiolo", 0.3546, 37.5822),
        c(12, "Meru", "Meru", 0.0471, 37.6498),
        c(13, "Tharaka-Nithi", "Chuka", -0.3333, 37.6500),
        c(14, "Embu", "Embu", -0.5389, 37.4596),
        c(15, "Kitui", "Kitui", -1.3667, 38.0106),
        c(16, "Machakos", "Machakos", -1.5177, 37.2634),
        c(17, "Makueni", "Wote", -1.7833, 37.6333),
        c(18, "Nyandarua", "Ol Kalou", -0.2700, 36.3800),
        // Landslide source: Kenya News Agency report "Man buried alive in Nyeri mudslide" (no specific area verified)
        c(19, "Nyeri", "Nyeri", -0.4201, 36.9476, landslideAreas = listOf()),
        c(20, "Kirinyaga", "Kerugoya", -0.4989, 37.2803),
        // Landslide source: Murang'a: EANSO study 'Elevation and Slope as Key Determinants of Landslide-Prone Areas in Murang'a County'; AllAfrica May 2026 on deadly Kangema and Mathioya landslides
        c(21, "Murang'a", "Murang'a", -0.7210, 37.1526, landslideAreas = listOf("Kangema", "Mathioya")),
        // Landslide source: Kimende escarpment: Kenya Red Cross landslide response 14 May 2024 (Citizen Digital, Kenyans.co.ke)
        c(22, "Kiambu", "Kiambu", -1.1714, 36.8356, landslideAreas = listOf("Kimende escarpment")),
        c(23, "Turkana", "Lodwar", 3.1191, 35.5973),
        // Landslide source: ReliefWeb "Death toll in West Pokot landslides rises to 24" (Nyarkulian and Parua villages)
        c(24, "West Pokot", "Kapenguria", 1.2389, 35.1119, landslideAreas = listOf("Nyarkulian", "Parua")),
        c(25, "Samburu", "Maralal", 1.0968, 36.6980),
        c(26, "Trans Nzoia", "Kitale", 1.0157, 35.0062),
        c(27, "Uasin Gishu", "Eldoret", 0.5143, 35.2698),
        // Landslide source: Chesongoch, Marakwet East landslide, Nov 2025: 26 dead (Reuters/AP, Africanews, Euronews)
        c(28, "Elgeyo-Marakwet", "Iten", 0.6703, 35.5081, landslideAreas = listOf("Marakwet East", "Chesongoch")),
        // Landslide source: Capital FM and Citizen Digital reports of fatal Nandi landslides; Kenya News Agency warning to residents of Tinderet
        c(29, "Nandi", "Kapsabet", 0.2039, 35.1050, landslideAreas = listOf("Tinderet", "Chepng'abai", "Sangalo")),
        c(30, "Baringo", "Kabarnet", 0.4919, 35.7430),
        c(31, "Laikipia", "Nanyuki", 0.0074, 37.0722),
        c(32, "Nakuru", "Nakuru", -0.3031, 36.0800),
        c(33, "Narok", "Narok", -1.0783, 35.8601, "Enkare Narok", -1.0783, 35.8601,
            listOf("Narok town", "Mosiro", "Ewaso Ngiro", "Kilgoris", "Nkareta")),
        c(34, "Kajiado", "Kajiado", -1.8524, 36.7768),
        // Landslide source: Kenya News Agency "3 children killed in Kipkelion landslide" (Kabirong, Kipkelion West)
        c(35, "Kericho", "Kericho", -0.3689, 35.2863, landslideAreas = listOf("Kipkelion West")),
        c(36, "Bomet", "Bomet", -0.7813, 35.3416),
        c(37, "Kakamega", "Kakamega", 0.2827, 34.7519),
        c(38, "Vihiga", "Mbale", 0.0833, 34.7167),
        // Landslide source: Kenyatta University study of Chetambe Hills landslide susceptibility (Webuye); Kenya-side Mt Elgon slopes are NOT yet verified
        c(39, "Bungoma", "Bungoma", 0.5635, 34.5606, landslideAreas = listOf("Chetambe Hills (Webuye)")),
        c(40, "Busia", "Busia", 0.4608, 34.1115, "Nzoia River", 0.1353, 34.0217, listOf("Budalangi")),
        c(41, "Siaya", "Siaya", 0.0612, 34.2881, "Nzoia River", 0.0700, 34.0900, listOf("Lower Nzoia", "Yala Swamp")),
        c(42, "Kisumu", "Kisumu", -0.0917, 34.7680, "Nyando River", -0.1739, 34.9186, listOf("Nyando", "Ahero", "Kano Plains")),
        c(43, "Homa Bay", "Homa Bay", -0.5273, 34.4571),
        c(44, "Migori", "Migori", -1.0634, 34.4731),
        c(45, "Kisii", "Kisii", -0.6817, 34.7680),
        c(46, "Nyamira", "Nyamira", -0.5633, 34.9358),
        c(47, "Nairobi", "Nairobi", -1.2864, 36.8172),
    )

    fun byCode(code: Int): County? = all.firstOrNull { it.code == code }
}
