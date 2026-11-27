# SnakeKit / 뱀 탐험대

Child-focused Korean offline illustrated snake encyclopedia. Version 1.1: 24 living taxa, original stylized color/pattern illustrations (not accurate identification artwork or real photos), searchable Korean/English/scientific names, habitat and family-group filters, local favorites, fact quizzes and safety education. No ads, network permission, analytics, account, external links or child data collection. Local favorites are stored only in WebView localStorage. Group filters are educational groupings rather than full phylogenetic taxonomy. Korean common names may vary; scientific names are included. Source URLs are displayed as text in detail view for adult reference. Source facts paraphrased, no source photos copied. Real photo licensing, content expansion, accessibility and real-device checks remain pending. Not a field identification or handling guide.

## 1.1 exploration update

Name-ranked search fixes the first CI failure. Adds four sand boas, four other living taxa, six tail feature cards, six morph concept illustrations, two extinct snakes and length comparisons. King cobra entry refers to the traditionally broad species complex. Fossil lengths are estimates; arbitrary comparator examples are labeled. No real photos yet; illustrations remain simplified. Source references are provided for adult verification. No ads or network. Device validation pending.

## 1.2

100 living taxa: 24 detailed entries and 76 basic taxonomic entries checked against Reptile Database genus lists (Eryx, Acanthophis, Crotalus, Hydrophis). Basic entries deliberately omit unverified species-specific distribution and morphology; Korean labels may be descriptive or scientific epithets. Quiz uses 24 detailed entries only. Collection and favorites stored locally. Eight-entry favorite-snake tournament. Native Korean offline TTS only, if installed; no network voices. Actual speech and UI require device testing. Photos optional via separately verified build pipeline; ordinary APK contains no photo files until a verified pack is supplied.

## 1.3

Adds level badges based on unique quiz discoveries, six curated photo candidates, four tail movement concept animations, explicitly synthetic warning-sound demonstrations, three source-backed living-species length examples and two dated conservation entries. Reduced-motion preference is honored; sound is user initiated, short, low volume and stops on navigation. Photo and extended catalog builds verify external data; ordinary 100-entry APK remains independent of external data fetches. Device/UI testing still required.

The extended photo workflow generates 400 additional basic entries at build time using GBIF Backbone Taxonomy (CC BY 4.0, https://doi.org/10.15468/39omei). Only accepted species of selected snake genera are included, exact scientific name duplicates excluded. Newly imported entries do not claim venom, appearance, distribution or conservation details. The generated dataset can change with upstream taxonomy; the workflow fails rather than padding if fewer than 400 valid entries are available. Standard APK is the 100-entry edition. Extended photo APK is a separately labeled artifact. The photo pipeline checks license, timestamp, file type, size and integrity before packaging. No device internet required.

## 1.3 build correction

The initial GBIF match returned no qualifying genera. The importer now uses an explicit backbone checklist search, exact accepted genus name, expected snake family, Animalia/Chordata, and numeric key, with diagnostic logging. Species still require accepted names in the same backbone and matching family and genus. The photo workflow independently builds base and extended editions so a catalog failure cannot cancel the 100-entry photo APK. All six configured photos must pass verification; no silent substitution.
