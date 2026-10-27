package ke.tahadhari.data.guides

/** Which service a number reaches. Labels for each language are in `Strings.emergencyService`. */
enum class EmergencyService { POLICE_FIRE_AMBULANCE, EMERGENCY_112, RED_CROSS }

/**
 * A national number shown on the Emergency screen. Only numbers checked against an official
 * source are listed; [sourceUrl] records where (re-check before every release).
 */
data class EmergencyContact(val number: String, val service: EmergencyService, val sourceUrl: String)

object EmergencyContacts {
    /** Verified 2026-10-27. */
    val national: List<EmergencyContact> = listOf(
        // National Police Service "Service standards" poster lists police hotlines 999/112/911.
        EmergencyContact(
            "999", EmergencyService.POLICE_FIRE_AMBULANCE,
            "https://nationalpolice.go.ke/sites/default/files/2025-07/NPS%20poster%20-%20SERVICE%20STANDARDS_0.pdf",
        ),
        EmergencyContact(
            "112", EmergencyService.EMERGENCY_112,
            "https://nationalpolice.go.ke/sites/default/files/2025-07/NPS%20poster%20-%20SERVICE%20STANDARDS_0.pdf",
        ),
        // Kenya Red Cross "Contact us": "Toll Free 1199".
        EmergencyContact("1199", EmergencyService.RED_CROSS, "https://www.redcross.or.ke/?p=56"),
    )

    // TODO(verify): National Disaster Operations Centre (NDOC). The only number found
    //  (+254 20 2212386) comes from a 2013 social media post, not a current official page.
    //  Do not show until it is confirmed on an official government site.
    // TODO(verify): county disaster / emergency lines. Collect from each county government's
    //  official site before adding them per county.
}
