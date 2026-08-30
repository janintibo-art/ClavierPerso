package com.perso.clavier

import android.graphics.Color

class Theme(
    val name: String,
    val bg: Int,
    val key: Int,
    val special: Int,
    val accent: Int,
    val text: Int,
    val textOnAccent: Int
)

object Themes {

    private fun c(hex: String) = Color.parseColor(hex)

    private fun t(
        name: String, bg: String, key: String, special: String,
        accent: String, text: String, textOnAccent: String = "#FFFFFF"
    ) = Theme(name, c(bg), c(key), c(special), c(accent), c(text), c(textOnAccent))

    val list: List<Theme> = listOf(
        t("Clair", "#E8EAED", "#FFFFFF", "#CDD3DC", "#4A6CF7", "#202124"),
        t("Sombre", "#1F2227", "#33373E", "#282C33", "#4A6CF7", "#E8EAED"),
        t("AMOLED", "#000000", "#16161A", "#0C0C0F", "#00C2A8", "#FFFFFF", "#00110E"),
        t("Océan", "#0E2A3F", "#1B4965", "#163B54", "#5FA8D3", "#E3F2FD", "#062033"),
        t("Forêt", "#16241B", "#2C4A34", "#21382A", "#7BC47F", "#E8F5E9", "#0E2412"),
        t("Sunset", "#2B1A2F", "#4A2545", "#3A1F3A", "#FF7B54", "#FFE9DC", "#33150A"),
        t("Rose", "#FCE4EC", "#FFFFFF", "#F3C1D3", "#E91E63", "#4A0E24"),
        t("Violet", "#1B1035", "#322153", "#271A44", "#9C6BFF", "#EDE7F6", "#1B1035"),
        t("Cyberpunk", "#0A0E17", "#131A2B", "#0E1422", "#00E5FF", "#E0F7FA", "#00232A"),
        t("Café", "#2A211C", "#4A3A30", "#3A2E26", "#C89F6E", "#F3E9DF", "#2A1D0F"),
        t("Menthe", "#E0F2F1", "#FFFFFF", "#B7DFD9", "#00897B", "#06302B"),
        t("Sable", "#F3EAD9", "#FFFDF7", "#E0D3B8", "#C98A2D", "#4A3B1E"),
        t("Nuit bleue", "#0D1B2A", "#1B2A41", "#14233A", "#FFC857", "#E0E6F0", "#1B2A41"),
        t("Rouge passion", "#1C0B0E", "#3A171D", "#2A1015", "#E63946", "#FFE5E8"),
        t("Lavande", "#EDE7F6", "#FFFFFF", "#CFC3E8", "#7E57C2", "#2E1A52"),
        t("Néon vert", "#0B0F0B", "#152015", "#101810", "#39FF14", "#E8FFE3", "#052500"),
        t("Anarchie", "#0A0A0A", "#1A0E0E", "#140A0A", "#E01B24", "#F5E6E6"),
        t("Gamer RGB", "#08080C", "#14141C", "#0E0E14", "#FF00E5", "#FFFFFF", "#12000F"),
        t("Or noir", "#101010", "#1E1E1E", "#171717", "#D4AF37", "#F5E9C8", "#1A1400"),
        t("Glace", "#DCE9F5", "#FFFFFF", "#C3D9EC", "#0288D1", "#0A2A42"),

        // Presets v36 : plus sobres, contrastés et cohérents avec l'identité Anarchie.
        // Ajoutés en fin de liste pour ne pas casser les thèmes déjà associés aux applications.
        t("Charbon", "#0D0D0F", "#202024", "#17171A", "#D53A43", "#F2F2F3"),
        t("Béton", "#202124", "#35363A", "#2B2C30", "#C81D25", "#F0F0F0"),
        t("Terminal", "#050806", "#101713", "#0B110D", "#66FF8A", "#D8FFE1", "#031007"),
        t("Verre rouge", "#0C0C10", "#26262C", "#1B1B20", "#FF3442", "#F8F8FA"),
        t("Industriel", "#111214", "#2B2D31", "#202226", "#FFB000", "#F3F3F3", "#231700"),
        t("Papier noir", "#171513", "#262320", "#201D1B", "#D94747", "#F1E9DF")
    )

    fun get(index: Int): Theme = list[index.coerceIn(0, list.size - 1)]
}
