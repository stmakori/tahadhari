package ke.tahadhari.data.guides

import ke.tahadhari.domain.Language

/** Picture shown next to a guide. Mapped to an icon in the UI. */
enum class GuideTopic { FLOODS, LANDSLIDES, CHOLERA, LIVESTOCK, LIGHTNING }

data class GuideSection(val title: String, val steps: List<String>)

/** One guide in one language. */
data class GuideContent(val title: String, val summary: String, val sections: List<GuideSection>)

/** Where the advice comes from. Shown as text at the bottom of the guide. */
data class GuideSource(val name: String, val url: String)

/**
 * An offline safety guide. Both languages must have the same sections and the same number of
 * steps per section (checked by `SafetyGuidesTest`), so ticks survive a language switch.
 */
data class SafetyGuide(
    val id: String,
    val topic: GuideTopic,
    val english: GuideContent,
    val swahili: GuideContent,
    val sources: List<GuideSource>,
) {
    fun content(language: Language): GuideContent = if (language == Language.SWAHILI) swahili else english
}

/**
 * Bundled safety guides, available without internet.
 * Facts and numbers were checked on 2026-10-27 against the sources listed with each guide.
 */
object SafetyGuides {
    private val krcsFloodTips = GuideSource(
        "Kenya Red Cross Society: flood safety tips (K24 Digital, March 2026)",
        "https://k24.digital/news/kenya-red-cross-issues-safety-tips-as-flooding-threat-persists/amp",
    )
    private val krcs = GuideSource("Kenya Red Cross Society", "https://www.redcross.or.ke")
    private val nwsTurnAround = GuideSource(
        "US National Weather Service: Turn Around Don't Drown",
        "https://www.weather.gov/safety/flood-turn-around-dont-drown",
    )
    private val nwsLightning = GuideSource(
        "US National Weather Service: Lightning safety",
        "https://www.weather.gov/safety/lightning-safety",
    )
    private val whoCholera = GuideSource(
        "World Health Organization: Cholera fact sheet",
        "https://www.who.int/news-room/fact-sheets/detail/cholera",
    )
    private val whoBoil = GuideSource(
        "World Health Organization: Boil water (technical brief)",
        "https://www.who.int/publications/i/item/WHO-FWC-WSH-15.02",
    )
    private val mohCholera = GuideSource(
        "Kenya Ministry of Health: cholera alert (People Daily, April 2025)",
        "https://peopledaily.digital/news/ministry-of-health-issues-urgent-alert-as-cholera-claims-6-lives",
    )
    private val usgsLandslide = GuideSource(
        "US Geological Survey: Signs of landslide development",
        "https://www.usgs.gov/programs/landslide-hazards/what-are-signs-landslide-development-what-do-i-do-if-a-landslide-occurs",
    )
    private val whoRvf = GuideSource(
        "World Health Organization: Rift Valley fever fact sheet",
        "https://www.who.int/news-room/fact-sheets/detail/rift-valley-fever",
    )
    private val kvaElNino = GuideSource(
        "Kenya Veterinary Association: El Niño livestock call (Citizen Digital)",
        "https://citizen.digital/article/veterinary-association-calls-for-urgent-action-to-shield-livestock-from-el-nino-n384049",
    )

    val floods = SafetyGuide(
        id = "floods",
        topic = GuideTopic.FLOODS,
        english = GuideContent(
            title = "Floods",
            summary = "Get ready before the rain, stay safe in rising water, and go home with care.",
            sections = listOf(
                GuideSection(
                    "Before a flood",
                    listOf(
                        "Know the way from your home to higher ground, and where your family will meet.",
                        "Save these numbers in your phone: 999, 112 and Kenya Red Cross 1199.",
                        "Pack a bag: water, food, torch, medicine, phone charger, and your ID in a plastic bag.",
                        "Keep your phone charged. Listen to the radio for weather warnings.",
                        "Clear drains and trenches around your home.",
                        "Plan how you will help children, older people and people with disabilities to leave.",
                    ),
                ),
                GuideSection(
                    "During a flood",
                    listOf(
                        "If water is rising or you are told to leave, go to higher ground at once.",
                        "If water enters the house, switch off electricity at the main switch.",
                        "Do not walk in moving water. 15 cm (ankle deep) can knock you down.",
                        "Do not drive through flood water or over a flooded bridge. 30 cm of moving water can sweep a car away.",
                        "If you must walk in water, test the depth with a stick before each step.",
                        "Stay away from fallen power lines and the water around them.",
                        "Keep children away from flood water, drains and rivers.",
                    ),
                ),
                GuideSection(
                    "After a flood",
                    listOf(
                        "Go home only when officials say it is safe.",
                        "Check walls and the roof for cracks before you go in.",
                        "Keep the power off until the wiring is dry and checked.",
                        "Boil or treat all drinking water. Flood water can carry cholera.",
                        "Clean and dry everything that got wet. Wash your hands with soap afterwards.",
                        "Sleep under a mosquito net. Standing water brings mosquitoes and malaria.",
                    ),
                ),
            ),
        ),
        swahili = GuideContent(
            title = "Mafuriko",
            summary = "Jiandae kabla ya mvua, kaa salama maji yakipanda, na urudi nyumbani kwa uangalifu.",
            sections = listOf(
                GuideSection(
                    "Kabla ya mafuriko",
                    listOf(
                        "Jua njia kutoka nyumbani kwenda sehemu ya juu, na mahali familia yako itakutana.",
                        "Hifadhi nambari hizi kwenye simu yako: 999, 112 na Msalaba Mwekundu 1199.",
                        "Andaa mfuko: maji, chakula, tochi, dawa, chaja ya simu, na kitambulisho chako ndani ya mfuko wa plastiki.",
                        "Weka simu yako ikiwa na chaji. Sikiliza redio kupata tahadhari za hali ya hewa.",
                        "Safisha mitaro na mifereji karibu na nyumba yako.",
                        "Panga jinsi utakavyowasaidia watoto, wazee na watu wenye ulemavu kuondoka.",
                    ),
                ),
                GuideSection(
                    "Wakati wa mafuriko",
                    listOf(
                        "Maji yakipanda au ukiambiwa uondoke, nenda sehemu ya juu mara moja.",
                        "Maji yakiingia nyumbani, zima umeme kwenye swichi kuu.",
                        "Usitembee kwenye maji yanayotiririka. Sentimita 15 (hadi kifundo cha mguu) zinaweza kukuangusha.",
                        "Usiendeshe gari kwenye maji ya mafuriko wala juu ya daraja lililofurika. Sentimita 30 za maji yanayotiririka zinaweza kusomba gari.",
                        "Ikibidi utembee majini, pima kina kwa fimbo kabla ya kila hatua.",
                        "Kaa mbali na nyaya za umeme zilizoanguka na maji yaliyo karibu nazo.",
                        "Weka watoto mbali na maji ya mafuriko, mitaro na mito.",
                    ),
                ),
                GuideSection(
                    "Baada ya mafuriko",
                    listOf(
                        "Rudi nyumbani tu maafisa wakisema ni salama.",
                        "Angalia kama kuta na paa zina nyufa kabla ya kuingia.",
                        "Usiwashe umeme hadi nyaya zikauke na zikaguliwe.",
                        "Chemsha au tibu maji yote ya kunywa. Maji ya mafuriko yanaweza kubeba kipindupindu.",
                        "Safisha na ukaushe kila kitu kilicholowa. Kisha osha mikono kwa sabuni.",
                        "Lala ndani ya chandarua. Maji yaliyotuama huleta mbu na malaria.",
                    ),
                ),
            ),
        ),
        sources = listOf(krcsFloodTips, nwsTurnAround, whoCholera),
    )

    val landslides = SafetyGuide(
        id = "landslides",
        topic = GuideTopic.LANDSLIDES,
        english = GuideContent(
            title = "Landslides",
            summary = "Know the warning signs on hills and slopes, and leave early.",
            sections = listOf(
                GuideSection(
                    "Warning signs",
                    listOf(
                        "New cracks in the ground, walls or roads.",
                        "Trees, poles or fences that start to lean.",
                        "Water coming out of a slope where it never did before.",
                        "A stream that suddenly rises, falls or turns muddy while it is raining.",
                        "Rumbling, cracking sounds or falling rocks.",
                    ),
                ),
                GuideSection(
                    "What to do",
                    listOf(
                        "If you see a warning sign, leave at once. Move to the side, out of the slide's path.",
                        "Warn your neighbours and the area chief.",
                        "During heavy rain, keep away from steep slopes, river banks and valley floors.",
                        "Do not sleep at the foot of a steep slope when heavy rain is forecast.",
                        "Do not go back after a slide. More slides can follow.",
                        "If people are trapped, call 999 or 1199. Do not dig into a slope that is still moving.",
                    ),
                ),
            ),
        ),
        swahili = GuideContent(
            title = "Maporomoko ya ardhi",
            summary = "Jua dalili za hatari kwenye milima na miteremko, na uondoke mapema.",
            sections = listOf(
                GuideSection(
                    "Dalili za hatari",
                    listOf(
                        "Nyufa mpya ardhini, kwenye kuta au barabarani.",
                        "Miti, nguzo au ua unaoanza kuinama.",
                        "Maji yanayotoka kwenye mteremko mahali ambapo hayajawahi kutoka.",
                        "Kijito kinachojaa ghafla, kupungua ghafla au kuwa na tope wakati mvua inanyesha.",
                        "Mngurumo, sauti za kupasuka au mawe yanayoanguka.",
                    ),
                ),
                GuideSection(
                    "Unachopaswa kufanya",
                    listOf(
                        "Ukiona dalili ya hatari, ondoka mara moja. Hamia pembeni, nje ya njia ya maporomoko.",
                        "Waonye majirani na chifu wa eneo.",
                        "Wakati wa mvua kubwa, kaa mbali na miteremko mikali, kingo za mito na mabonde.",
                        "Usilale chini ya mteremko mkali mvua kubwa ikitabiriwa.",
                        "Usirudi baada ya maporomoko. Maporomoko mengine yanaweza kufuata.",
                        "Watu wakinaswa, piga 999 au 1199. Usichimbe mteremko ambao bado unasogea.",
                    ),
                ),
            ),
        ),
        sources = listOf(usgsLandslide, krcs),
    )

    val cholera = SafetyGuide(
        id = "cholera",
        topic = GuideTopic.CHOLERA,
        english = GuideContent(
            title = "Cholera and safe water",
            summary = "Floods spread cholera. Safe water, clean hands and fast treatment save lives.",
            sections = listOf(
                GuideSection(
                    "Safe water",
                    listOf(
                        "Drink only water that is boiled or treated with chlorine.",
                        "To boil: heat the water until it bubbles strongly, then let it cool, covered.",
                        "To use chlorine tablets or liquid, follow the packet and wait the time it says before drinking.",
                        "Never drink water from rivers, lakes or floods without treating it.",
                        "Store water in a clean, covered container. Pour it out; don't dip cups in.",
                    ),
                ),
                GuideSection(
                    "Clean hands and food",
                    listOf(
                        "Wash hands with soap after the toilet, before cooking or eating, and after caring for a sick person.",
                        "Eat food that is well cooked and still hot.",
                        "Wash fruit with safe water, or peel it yourself.",
                        "Use a toilet or latrine. Keep faeces away from water sources.",
                    ),
                ),
                GuideSection(
                    "If someone gets sick",
                    listOf(
                        "Cholera causes sudden watery diarrhoea, often with vomiting. It can kill within hours.",
                        "Start ORS at once: mix a sachet with safe water as the packet says, and give small sips often.",
                        "Keep breastfeeding a sick baby.",
                        "Go to the nearest health facility now. Keep giving ORS on the way.",
                        "Get help fast if the person is very weak, cannot drink, has sunken eyes or passes little urine.",
                    ),
                ),
            ),
        ),
        swahili = GuideContent(
            title = "Kipindupindu na maji safi",
            summary = "Mafuriko hueneza kipindupindu. Maji safi, mikono safi na matibabu ya haraka huokoa maisha.",
            sections = listOf(
                GuideSection(
                    "Maji salama",
                    listOf(
                        "Kunywa maji yaliyochemshwa au kutibiwa kwa klorini pekee.",
                        "Kuchemsha: pasha maji hadi yachemke kwa nguvu, kisha yaache yapoe yakiwa yamefunikwa.",
                        "Ukitumia vidonge au dawa ya klorini, fuata maelekezo ya pakiti na usubiri muda uliotajwa kabla ya kunywa.",
                        "Usinywe kamwe maji ya mito, maziwa au mafuriko bila kuyatibu.",
                        "Hifadhi maji kwenye chombo safi kilichofunikwa. Yamimine; usichote kwa kikombe.",
                    ),
                ),
                GuideSection(
                    "Mikono na chakula safi",
                    listOf(
                        "Osha mikono kwa sabuni baada ya kutoka chooni, kabla ya kupika au kula, na baada ya kumhudumia mgonjwa.",
                        "Kula chakula kilichopikwa vizuri na bado ni cha moto.",
                        "Osha matunda kwa maji salama, au uyamenye mwenyewe.",
                        "Tumia choo. Weka kinyesi mbali na vyanzo vya maji.",
                    ),
                ),
                GuideSection(
                    "Mtu akiugua",
                    listOf(
                        "Kipindupindu husababisha kuharisha maji ghafla, mara nyingi pamoja na kutapika. Kinaweza kuua ndani ya saa chache.",
                        "Anza ORS mara moja: changanya pakiti na maji salama kama pakiti inavyosema, na umpe kidogo kidogo mara kwa mara.",
                        "Endelea kumnyonyesha mtoto mgonjwa.",
                        "Nenda kituo cha afya kilicho karibu sasa hivi. Endelea kumpa ORS njiani.",
                        "Tafuta msaada haraka mtu akiwa dhaifu sana, hawezi kunywa, macho yamezama au anakojoa kidogo.",
                    ),
                ),
            ),
        ),
        sources = listOf(whoCholera, mohCholera, whoBoil),
    )

    val livestock = SafetyGuide(
        id = "livestock",
        topic = GuideTopic.LIVESTOCK,
        english = GuideContent(
            title = "Livestock and farms",
            summary = "Protect animals, feed and seed before the water comes.",
            sections = listOf(
                GuideSection(
                    "Before the floods",
                    listOf(
                        "Move animals to higher ground early, before roads and paths flood.",
                        "Store feed, hay and seed in a dry, raised place, off the floor.",
                        "Vaccinate animals early, as your county vet advises. Rift Valley fever spreads after heavy rain.",
                        "Harvest ripe crops early if you can.",
                    ),
                ),
                GuideSection(
                    "During and after",
                    listOf(
                        "Give animals clean water. Do not let them drink flood water.",
                        "Report sick or dying animals to the county veterinary officer.",
                        "Do not slaughter sick animals. Wear gloves if you must handle them or their blood.",
                        "Boil milk and cook meat well before eating.",
                        "Bury dead animals away from water sources.",
                        "Sleep under a mosquito net. Mosquitoes spread Rift Valley fever and malaria.",
                    ),
                ),
            ),
        ),
        swahili = GuideContent(
            title = "Mifugo na mashamba",
            summary = "Linda mifugo, chakula cha mifugo na mbegu kabla maji hayajafika.",
            sections = listOf(
                GuideSection(
                    "Kabla ya mafuriko",
                    listOf(
                        "Hamisha mifugo kwenda sehemu ya juu mapema, kabla barabara na njia hazijafurika.",
                        "Hifadhi chakula cha mifugo, nyasi kavu na mbegu mahali pakavu palipoinuliwa, si sakafuni.",
                        "Chanja mifugo mapema kama daktari wa mifugo wa kaunti anavyoshauri. Homa ya Bonde la Ufa huenea baada ya mvua kubwa.",
                        "Vuna mazao yaliyokomaa mapema ukiweza.",
                    ),
                ),
                GuideSection(
                    "Wakati na baada ya mafuriko",
                    listOf(
                        "Wape mifugo maji safi. Usiwaache wanywe maji ya mafuriko.",
                        "Ripoti mifugo wagonjwa au wanaokufa kwa afisa wa mifugo wa kaunti.",
                        "Usichinje mifugo wagonjwa. Vaa glavu ikibidi uwashike au uguse damu yao.",
                        "Chemsha maziwa na upike nyama vizuri kabla ya kula.",
                        "Zika mifugo waliokufa mbali na vyanzo vya maji.",
                        "Lala ndani ya chandarua. Mbu hueneza homa ya Bonde la Ufa na malaria.",
                    ),
                ),
            ),
        ),
        sources = listOf(whoRvf, kvaElNino),
    )

    val lightning = SafetyGuide(
        id = "lightning",
        topic = GuideTopic.LIGHTNING,
        english = GuideContent(
            title = "Lightning and flash floods",
            summary = "Storms bring sudden water and lightning. Act fast, and never cross moving water.",
            sections = listOf(
                GuideSection(
                    "Flash floods",
                    listOf(
                        "Never cross moving water, on foot or by car. Turn back.",
                        "15 cm of moving water can knock you down.",
                        "30 cm can sweep a car away. 60 cm can carry away lorries and other big vehicles.",
                        "Dry riverbeds (lagas) can fill in minutes from rain far away. Do not camp, park or herd animals in them.",
                        "If you hear a roar of water upstream, climb to higher ground at once.",
                    ),
                ),
                GuideSection(
                    "Lightning",
                    listOf(
                        "When you hear thunder, go inside a solid building or a car with a metal roof.",
                        "Do not shelter under a tree. Stay off hilltops, open fields and water.",
                        "Indoors, keep away from windows and do not use plugged-in devices.",
                        "Wait 30 minutes after the last thunder before going outside.",
                        "If someone is struck, call 999 and give first aid. It is safe to touch them.",
                    ),
                ),
            ),
        ),
        swahili = GuideContent(
            title = "Radi na mafuriko ya ghafla",
            summary = "Dhoruba huleta maji ya ghafla na radi. Chukua hatua haraka, na usivuke kamwe maji yanayotiririka.",
            sections = listOf(
                GuideSection(
                    "Mafuriko ya ghafla",
                    listOf(
                        "Usivuke kamwe maji yanayotiririka, kwa miguu wala kwa gari. Rudi nyuma.",
                        "Sentimita 15 za maji yanayotiririka zinaweza kukuangusha.",
                        "Sentimita 30 zinaweza kusomba gari. Sentimita 60 zinaweza kusomba malori na magari mengine makubwa.",
                        "Mito iliyokauka (laga) inaweza kujaa kwa dakika chache kutokana na mvua ya mbali. Usipige kambi, usiegeshe gari wala usichunge mifugo ndani yake.",
                        "Ukisikia mngurumo wa maji upande wa juu wa mto, panda sehemu ya juu mara moja.",
                    ),
                ),
                GuideSection(
                    "Radi",
                    listOf(
                        "Ukisikia ngurumo, ingia ndani ya jengo imara au gari lenye paa la chuma.",
                        "Usijikinge chini ya mti. Kaa mbali na vilele vya milima, mashamba wazi na maji.",
                        "Ukiwa ndani, kaa mbali na madirisha na usitumie vifaa vilivyochomekwa kwenye umeme.",
                        "Subiri dakika 30 baada ya ngurumo ya mwisho kabla ya kutoka nje.",
                        "Mtu akipigwa na radi, piga 999 na umpe huduma ya kwanza. Ni salama kumgusa.",
                    ),
                ),
            ),
        ),
        sources = listOf(nwsTurnAround, nwsLightning, krcsFloodTips),
    )

    val all: List<SafetyGuide> = listOf(floods, landslides, cholera, livestock, lightning)

    fun byId(id: String): SafetyGuide? = all.firstOrNull { it.id == id }
}
