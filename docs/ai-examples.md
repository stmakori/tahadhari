# AI explanation examples

Generated 2026-11-11 by `AiExamplesTest` from live Open-Meteo forecasts, through the AI server at `https://tahadhari-ai.onrender.com`
(model: the server's `TAHADHARI_MODEL`, default `claude-haiku-5-5`). Regenerate with:

```bash
ANTHROPIC_API_KEY=sk-... ./gradlew :server:run
TAHADHARI_AI_EXAMPLES=1 ./gradlew :composeApp:desktopTest --tests "ke.tahadhari.AiExamplesTest"
```

The risk level always comes from `RiskEngine`; the AI only writes the words. "Numbers not in the facts"
flags any number in the reply that the app did not send, so a reviewer can check it was not invented.
Tick each review box only after reading the example; Swahili needs a fluent speaker.

| County | Level | Reasons | English | Swahili |
|---|---|---|---|---|
| Narok | SEVERE | RIVER_RISING, HEAVY_DAY, WET_SPELL | AI | AI |
| Taita-Taveta | HIGH | LANDSLIDE, HEAVY_DAY, WET_SPELL | AI | AI |
| Kericho | HIGH | LANDSLIDE, HEAVY_DAY, WET_SPELL | AI | AI |
| Nyeri | HIGH | LANDSLIDE, HEAVY_DAY, WET_SPELL | AI | AI |
| Kisii | MODERATE | HEAVY_DAY, WET_SPELL | AI | AI |
| Homa Bay | MODERATE | HEAVY_DAY, WET_SPELL | AI | AI |
| Nyamira | MODERATE | HEAVY_DAY, WET_SPELL | AI | AI |
| Trans Nzoia | MODERATE | HEAVY_DAY, WET_SPELL | AI | AI |
| Kakamega | MODERATE | WET_SPELL | AI | AI |
| Mandera | LOW | – | AI | AI |
| Kilifi | LOW | – | AI | AI |
| Mombasa | LOW | – | AI | AI |

## Narok: SEVERE

Facts sent to the AI (English request):

```text
Language: English
County: Narok (main town: Narok)
Risk level: SEVERE
Reasons: RIVER_RISING, HEAVY_DAY, WET_SPELL
Wettest day: Thursday 2026-11-12, about 27 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 49 mm
River: Enkare Narok forecast to peak at 7.0x its recent level on Sunday
Known flood/landslide hotspots: Narok town, Mosiro, Ewaso Ngiro, Kilgoris, Nkareta
```

### English

**Severe flood risk in Narok: act now and move to safety**

The Enkare Narok river is forecast to rise sharply, peaking around 7 times its recent level on Sunday, and heavy rain is expected on Thursday 12 November 2026 with a 100% chance of rain. Hotspots include Narok town, Mosiro, Ewaso Ngiro, Kilgoris and Nkareta, so stay away from river banks and low-lying ground now.

- Move your family, valuables and livestock away from the river and low-lying areas before Thursday.
- Keep your phone charged and follow updates from local authorities.
- Avoid crossing flooded roads or bridges; turn around, don't drown.
- In an emergency, call 999 or 112, or Kenya Red Cross on 1199.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari kubwa ya mafuriko Narok: chukua hatua sasa**

Mto Enkare Narok unatarajiwa kufika kilele Jumapili, ukiwa juu mara 7.0 ya kiwango chake cha sasa. Alhamisi tarehe 12 Novemba 2026 kutakuwa na mvua kubwa, takriban 27 mm, na mvua inatarajiwa kunyesha kwa siku tatu mfululizo, jumla ya hadi 49 mm. Maeneo yanayokumbwa na mafuriko ni Narok mjini, Mosiro, Ewaso Ngiro, Kilgoris na Nkareta.

- Hama mara moja kutoka kando ya mto Enkare Narok na maeneo ya chini ya mto kabla ya Jumapili.
- Hifadhi hati muhimu, dawa na chakula kwenye mfuko usiopitisha maji, na weka simu ikichajiwa.
- Epuka kuvuka madaraja au mikondo ya maji, hata kama maji yanaonekana madogo.
- Piga 999 au 112 kwa msaada wa dharura, au 1199 kwa Msalaba Mwekundu wa Kenya.

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Taita-Taveta: HIGH

Facts sent to the AI (English request):

```text
Language: English
County: Taita-Taveta (main town: Wundanyi)
Risk level: HIGH
Reasons: LANDSLIDE, HEAVY_DAY, WET_SPELL
Wettest day: Saturday 2026-11-14, about 36 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 61 mm
```

### English

**High flood and landslide risk in Taita-Taveta, get ready to move**

Heavy rain is forecast in Taita-Taveta, with about 36 mm expected on Saturday 14 November 2026 and up to 61 mm over three days in the coming week. The steep, landslide-prone slopes around Wundanyi make landslides a real danger, so prepare now and be ready to leave quickly if the ground starts to change.

- Watch for cracks in the ground, tilting trees or leaning poles, and new muddy water flowing down slopes.
- Move away from steep slopes and from valley bottoms below them, especially at night.
- Keep your phone charged, your important papers together and a bag ready to leave.
- Do not return to an area until it has been checked. In an emergency call 999, 112 or 1199 (Kenya Red Cross).

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari kubwa ya maporomoko ya ardhi Taita-Taveta, jiandae kuhama**

Mvua kubwa inatarajiwa Taita-Taveta, hasa Jumamosi tarehe 14 Novemba 2026, ambapo kiwango cha mvua kinaweza kufikia takriban mm 36, na jumla ya mvua ya siku 3 inaweza kufikia mm 61. Kwa sababu ya mteremko mkali, hatari ya maporomoko ya ardhi ni kubwa, kwa hivyo jiandae kuhama mapema ikiwa unaishi karibu na miteremko au chini ya milima.

- Tafuta mahali salama mbali na miteremko mikali na mabonde chini ya milima, hasa usiku.
- Angalia nyufa ardhini, miti au nguzo zinazoinama; ukiviona, ondoka mara moja.
- Usirudi nyumbani hadi eneo likaguliwe na mamlaka zinazohusika.
- Ukiwa katika hatari au unahitaji msaada, piga 999 au 112, au Msalaba Mwekundu Kenya 1199.

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Kericho: HIGH

Facts sent to the AI (English request):

```text
Language: English
County: Kericho (main town: Kericho)
Risk level: HIGH
Reasons: LANDSLIDE, HEAVY_DAY, WET_SPELL
Wettest day: Saturday 2026-11-14, about 28 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 60 mm
```

### English

**Kericho: high flood and landslide risk, get ready to move**

Heavy rain is forecast in Kericho, with about 28 mm expected on Saturday 14 November and up to 60 mm over three days this week. The steep, landslide-prone slopes around the county make the danger serious, so prepare to move to higher, safer ground if needed.

- Keep your bags packed and important documents in one place, ready to leave quickly.
- Stay away from steep slopes and from valley bottoms below them, especially at night.
- Watch for cracks in the ground, tilting trees or poles, and water suddenly running muddy.
- If you see these signs or feel unsafe, leave at once and call 999, 112 or 1199 (Kenya Red Cross).

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari kubwa ya maporomoko ya ardhi Kericho. Jiandae kuhama.**

Mvua kubwa inatarajiwa Kericho, ikiwa na kiwango cha juu cha mvua ya siku tatu cha takriban 60 mm katika siku saba zijazo, na Jumamosi 2026-11-14 ndiyo siku yenye mvua nyingi, takriban 28 mm. Kwa kuwa eneo hili lina miteremko mikali, maporomoko ya ardhi yanaweza kutokea, hasa usiku. Tahadhari hii ni mwongozo tu, si tangazo rasmi.

- Hama mapema kutoka miteremko mikali na sehemu za chini ya mteremko, hasa usiku.
- Angalia nyufa ardhini au miti na nguzo zinazoinama, na uondoke mara unapoziona.
- Usirudi nyumbani hadi eneo likaguliwe na mamlaka husika.
- Piga 999 au 112 kwa msaada wa dharura, au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Nyeri: HIGH

Facts sent to the AI (English request):

```text
Language: English
County: Nyeri (main town: Nyeri)
Risk level: HIGH
Reasons: LANDSLIDE, HEAVY_DAY, WET_SPELL
Wettest day: Saturday 2026-11-14, about 23 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 56 mm
```

### English

**Nyeri: high flood and landslide risk, get ready to move**

Heavy rain is forecast in Nyeri, with about 23 mm expected on Saturday 14 November 2026 and up to 56 mm over three days within the next week. Because the slopes are steep and prone to landslides, the danger is greatest on hillsides and in valleys below them, especially at night.

- Stay away from steep slopes and the valley bottoms below them, especially at night.
- Watch for new cracks in the ground, tilting trees or poles, and water seeping from slopes, and leave at once if you see them.
- Prepare a bag with documents, medicine, water and a torch, and plan where to go if you must move quickly.
- Do not return to an area until it has been checked. In an emergency call 999 or 112, or 1199 for the Kenya Red Cross.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari kubwa ya maporomoko ya ardhi Nyeri: jiandae kuhama**

Mvua kubwa inatarajiwa Nyeri, na jumla ya mvua ya siku tatu inaweza kufikia milimita 56, hasa siku ya Jumamosi 2026-11-14 yenye mvua ya karibu milimita 23. Kwa sababu ya miteremko mikali, hatari ya maporomoko ya ardhi ni kubwa, hasa usiku. Tahadhari hii ni mwongozo tu, si tangazo rasmi.

- Angalia nyufa ardhini, miti au nguzo zinazoinama, na maji yanayotiririka kwa ghafla.
- Hama mbali na miteremko mikali na sehemu za chini ya mteremko, hasa usiku.
- Usirudi nyumbani hadi eneo likaguliwe na mamlaka za eneo lako.
- Ukiwa hatarini, piga 999 au 112, au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Kisii: MODERATE

Facts sent to the AI (English request):

```text
Language: English
County: Kisii (main town: Kisii)
Risk level: MODERATE
Reasons: HEAVY_DAY, WET_SPELL
Wettest day: Friday 2026-11-13, about 26 mm, chance of rain 97%
Highest 3-day rainfall total in the next 7 days: 61 mm
```

### English

**Kisii: Take care, heavy rain expected this week**

Kisii town is forecast to get a very wet day on Friday 13 November 2026, with about 26 mm of rain and a high chance of rain. A wet spell is also expected, with up to 61 mm over three days in the next seven days, so keep an eye on local conditions.

- Avoid crossing flooded roads, footbridges or streams on foot or by vehicle.
- Keep valuables and important documents in a high, dry place.
- Clear drains and gutters near your home so water can run away.
- Check local updates regularly and keep a phone charged for emergency calls on 999, 112 or 1199 (Kenya Red Cross).

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Kisii: Tahadhari ya wastani, kuwa mwangalifu**

Mvua kubwa inatarajiwa Kisii, hasa siku ya Ijumaa tarehe 13 Novemba 2026, ambapo mvua inaweza kufikia takriban milimita 26. Katika siku tatu zijazo, jumla ya mvua inaweza kufikia milimita 61, kwa hivyo kuwa mwangalifu na uwe tayari.

- Epuka kuvuka mito, mifereji na madaraja yaliyojaa maji.
- Hifadhi vitu vya thamani na nyaraka muhimu mahali palipo juu na pakavu.
- Fuatilia taarifa mpya kutoka kwa mamlaka rasmi kila siku.
- Ukihitaji msaada wa dharura, piga 999, 112, au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Homa Bay: MODERATE

Facts sent to the AI (English request):

```text
Language: English
County: Homa Bay (main town: Homa Bay)
Risk level: MODERATE
Reasons: HEAVY_DAY, WET_SPELL
Wettest day: Thursday 2026-11-12, about 32 mm, chance of rain 94%
Highest 3-day rainfall total in the next 7 days: 59 mm
```

### English

**Moderate flood risk in Homa Bay; take care this week**

Heavy rain is expected on Thursday 12 November 2026, with about 32 mm and a 94% chance of rain. A wet spell of up to 59 mm over three days is also possible in the next seven days, so flooding of low-lying roads and areas near streams is possible in Homa Bay town and nearby areas.

- Avoid crossing flooded roads or footbridges on foot or by vehicle.
- Keep valuables, documents and medicines in a raised, dry place.
- Keep your phone charged and check for updates from local authorities.
- In an emergency, call 999 or 112, or the Kenya Red Cross on 1199.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Homa Bay: Kuwa mwangalifu, mvua kubwa inakuja**

Kaunti ya Homa Bay, hasa mji wa Homa Bay, inatarajiwa kupata mvua kubwa siku ya Alhamisi, 12 Novemba 2026, kiasi cha takriban 32 mm, na nafasi ya mvua ni 94%. Katika siku 7 zijazo, jumla ya mvua ya siku 3 mfululizo inaweza kufikia 59 mm, kwa hivyo kuwa mwangalifu.

- Epuka kuvuka mito, mifereji na madimbwi ya maji wakati wa mvua.
- Hakikisha unajua njia salama ya kutoka nyumbani kwako na uwe tayari kuhama mapema.
- Weka vitu muhimu, hati za kibinafsi na dawa mahali pakavu na palipoinuka.
- Kwa dharura, piga 999 au 112, au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Nyamira: MODERATE

Facts sent to the AI (English request):

```text
Language: English
County: Nyamira (main town: Nyamira)
Risk level: MODERATE
Reasons: HEAVY_DAY, WET_SPELL
Wettest day: Wednesday 2026-11-11, about 21 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 57 mm
```

### English

**Moderate flood risk in Nyamira: take care this week**

Heavy rain is expected in Nyamira, with the wettest day on Wednesday 11 November 2026 at about 21 mm and a chance of rain of 100%. Rain is also expected to continue for several days, with up to 57 mm over three days in the next 7 days, so ground and drains may fill up quickly.

- Clear drains, gutters and culverts near your home so water can flow away.
- Avoid crossing flooded roads, footpaths or streams on foot or by vehicle.
- Keep torches, drinking water, important documents and phone charge ready in case you need to move.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Tahadhari ya mvua kubwa Nyamira: chukua tahadhari**

Kaunti ya Nyamira, hasa mji wa Nyamira, inatarajia mvua kubwa siku ya Jumatano 2026-11-11, kiasi cha takriban mm 21, na mvua inaweza kunyesha kwa siku tatu mfululizo, jumla ya takriban mm 57. Hali hii ni ya tahadhari ya wastani, kwa hivyo jiandae mapema na uepuke kuvuka mifereji au mito iliyojaa maji.

- Hifadhi vitu vya thamani na nyaraka muhimu mahali pakavu na palipoinuka.
- Epuka kuvuka mifereji, madaraja madogo na mito yenye maji mengi.
- Fuatilia taarifa mpya kutoka kwa mamlaka rasmi za hali ya hewa na uongozi wa eneo lako.
- Ikiwa unahitaji msaada wa dharura, piga 999 au 112, au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Trans Nzoia: MODERATE

Facts sent to the AI (English request):

```text
Language: English
County: Trans Nzoia (main town: Kitale)
Risk level: MODERATE
Reasons: HEAVY_DAY, WET_SPELL
Wettest day: Saturday 2026-11-14, about 21 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 55 mm
```

### English

**Moderate flood risk in Trans Nzoia: take care this week**

Heavy rain is expected in Trans Nzoia, with the wettest day on Saturday 14 November 2026 at about 21 mm and up to 55 mm over three days in the next week. Because of this, rivers, low-lying areas and roads near Kitale may flood, so take care when moving around.

- Avoid crossing flooded roads or rivers on foot or by vehicle, even when the water looks shallow.
- Move valuables, food and important documents to higher ground before Saturday.
- Keep your phone charged and check local updates often.
- In an emergency, call 999 or 112, or the Kenya Red Cross on 1199.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari ya mafuriko ya wastani Trans Nzoia; kuwa makini**

Kaunti ya Trans Nzoia, hasa mji wa Kitale, inatarajia mvua kubwa siku ya Jumamosi, tarehe 14 Novemba 2026, kiasi cha takriban mm 21, na mvua inaweza kunyesha kwa siku kadhaa. Jumla ya mvua ya siku tatu inaweza kufikia mm 55 katika siku saba zijazo. Kwa hivyo, jiandae mapema na uwe makini na maji yanayopanda.

- Epuka kuvuka mito, mifereji na madimbwi yenye maji yanayotiririka.
- Hifadhi nyaraka muhimu, dawa na chakula kavu mahali pakavu na salama.
- Fuatilia taarifa mpya kutoka kwa mamlaka rasmi, na ukiwa na dharura piga 999, 112 au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Kakamega: MODERATE

Facts sent to the AI (English request):

```text
Language: English
County: Kakamega (main town: Kakamega)
Risk level: MODERATE
Reasons: WET_SPELL
Wettest day: Saturday 2026-11-14, about 19 mm, chance of rain 100%
Highest 3-day rainfall total in the next 7 days: 51 mm
```

### English

**Wet spell expected in Kakamega: take care this week**

Kakamega town and the county are forecast to have a wet spell, with the heaviest rain on Saturday 14 November 2026, about 19 mm, and up to 51 mm over three days within the next week. Rain is likely, so keep drains clear and be cautious around swollen streams and low crossings.

- Clear drains and gutters near your home so water can flow away.
- Avoid walking or driving through flooded roads, streams or low crossings.
- Keep important documents, medicines and phone charging ready in case you need to move.
- Check on neighbours who are elderly or live near rivers or low ground.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Mvua ya kati inatarajiwa Kakamega; chukua tahadhari**

Kaunti ya Kakamega, hasa mji wa Kakamega, inatarajia kipindi cha mvua kwa siku tatu, na jumla ya mvua inaweza kufikia milimita 51. Siku yenye mvua nyingi zaidi ni Jumamosi tarehe 14 Novemba 2026, yenye takriban milimita 19 na uwezekano wa mvua wa asilimia 100.

- Safisha mifereji ya maji karibu na nyumba yako ili maji yapite vizuri.
- Hifadhi vitu vya thamani na hati muhimu mahali pakavu na juu zaidi.
- Epuka kuvuka mito au madimbwi yenye maji yanayotiririka.
- Kwa dharura piga 999, 112 au 1199 (Msalaba Mwekundu wa Kenya).

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Mandera: LOW

Facts sent to the AI (English request):

```text
Language: English
County: Mandera (main town: Mandera)
Risk level: LOW
Wettest day: Thursday 2026-11-12, about 6 mm, chance of rain 75%
Highest 3-day rainfall total in the next 7 days: 13 mm
```

### English

**Low flood risk in Mandera; stay prepared**

Rain is expected in Mandera over the next week, but flood risk is low at present. The wettest day is Thursday 12 November, with about 6 mm of rain and a 75% chance of rain.

- Keep an eye on rain updates for Mandera town over the coming days.
- Keep drains and gutters near your home clear of rubbish so water can flow away.
- Keep a torch, phone charger and drinking water at home in case conditions change.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hali ya mvua Mandera ni ya chini; jiandae mapema**

Kaunti ya Mandera, hasa mji wa Mandera, ina hatari ndogo ya mafuriko katika siku saba zijazo. Siku yenye mvua nyingi zaidi itakuwa Alhamisi 2026-11-12, takriban milimita 6, na nafasi ya mvua ni asilimia 75. Jumla ya mvua ya siku tatu zijazo inaweza kufikia milimita 13, kwa hivyo endelea kufuatilia taarifa.

- Fuatilia taarifa za hali ya hewa kila siku, hasa Alhamisi 2026-11-12.
- Hakikisha mifereji ya maji karibu na nyumba yako iko wazi.
- Weka mahali salama vitu muhimu kama hati, dawa na chakula kidogo.
- Ukisikia mvua kubwa au maji yanapanda, nenda sehemu ya juu na piga 112 au 999 kwa msaada.

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Kilifi: LOW

Facts sent to the AI (English request):

```text
Language: English
County: Kilifi (main town: Kilifi)
Risk level: LOW
Wettest day: Saturday 2026-11-14, about 6 mm, chance of rain 96%
Highest 3-day rainfall total in the next 7 days: 12 mm
```

### English

**Low flood risk in Kilifi; stay prepared**

Kilifi faces a low flood risk right now. The wettest day is expected to be Saturday 14 November 2026, with about 6 mm of rain and a 96% chance of rain, and the highest 3-day total over the next week is about 12 mm. Keep an eye on the forecast and be ready if conditions change.

- Keep your phone charged and check updates from local authorities.
- Clear drains and gutters near your home so water can flow away.
- Keep important documents and medicines in a dry, high place.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hatari ndogo ya mafuriko Kilifi, endelea kuwa makini**

Kaunti ya Kilifi, hasa mji wa Kilifi, ina hatari ndogo ya mafuriko katika siku saba zijazo. Siku yenye mvua nyingi zaidi itakuwa Jumamosi, 14 Novemba 2026, ambapo mvua inatarajiwa kuwa karibu mm 6, na mvua ya siku tatu inaweza kufikia karibu mm 12. Hakuna haja ya kuhama, lakini ni vizuri kujiandaa mapema.

- Fuatilia taarifa mpya za hali ya hewa kila siku.
- Safisha mitaro na njia za maji karibu na nyumba yako.
- Weka vitu muhimu na hati zako mahali salama na pakavu.
- Epuka kuvuka maji yanayotiririka kwa kasi.

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts

## Mombasa: LOW

Facts sent to the AI (English request):

```text
Language: English
County: Mombasa (main town: Mombasa)
Risk level: LOW
Wettest day: Saturday 2026-11-14, about 3 mm, chance of rain 90%
Highest 3-day rainfall total in the next 7 days: 9 mm
```

### English

**Low flood risk in Mombasa this week; stay prepared**

Rain in Mombasa is expected to stay light, with the wettest day, Saturday 14 November 2026, bringing only about 3 mm, and the highest 3-day total over the next week is about 9 mm. This is a low risk, so there is no need to change your plans, but it is still wise to keep an eye on the weather. Keep drains near your home clear of rubbish so water can flow away easily.

- Clear rubbish from drains and gutters near your home.
- Keep an eye on the weather and check for updates before Saturday.
- Keep a torch and phone charger ready in case the weather changes.

Automatic check: every number is in the facts.

- [ ] Reviewed: accurate, calm, matches the level, no invented facts

### Kiswahili

**Hali ya mvua Mombasa ni ndogo kwa sasa**

Kaunti ya Mombasa inatarajiwa kupata mvua ndogo wiki hii. Siku yenye mvua nyingi zaidi ni Jumamosi tarehe 14 Novemba 2026, takriban milimita 3, na uwezekano wa mvua ni asilimia 90. Jumla ya mvua ya siku tatu mfululizo inatarajiwa kufika milimita 9 tu.

- Endelea kufuatilia taarifa mpya za hali ya hewa kila siku.
- Hakikisha mifereji ya maji karibu na nyumba yako iko wazi.
- Weka mahali salama vitu vya thamani kama hati muhimu na simu.

Automatic check: every number is in the facts.

- [ ] Reviewed by a fluent Swahili speaker: accurate, calm, matches the level, no invented facts
