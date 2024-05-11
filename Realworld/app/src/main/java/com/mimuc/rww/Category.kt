import com.mimuc.rww.R

enum class Category(
    var value: String,
    var iconLarge: Int = 0,
    var iconSmall: Int = 0,
    var img: Int = 0)
{
    RELAXING("Relaxing", R.drawable.ic_icon_relax, R.drawable.ic_small_relax, R.drawable.ic_img_relax),
    MENTAL("Mental", R.drawable.ic_icon_mental, R.drawable.ic_small_mental, R.drawable.ic_img_mental),
    PHYSICAL("Physical", R.drawable.ic_icon_physical, R.drawable.ic_small_physical, R.drawable.ic_img_physical),
    SOCIAL("Social", R.drawable.ic_icon_social, R.drawable.ic_small_social, R.drawable.ic_img_social),
    ORGANIZING("Organizing", R.drawable.ic_icon_organizing, R.drawable.ic_small_organizing, R.drawable.ic_img_organizing),
    MISC("Misc", R.drawable.ic_icon_misc, R.drawable.ic_small_misc, R.drawable.ic_img_misc),

    PERSONALIZED("Personalized"),
    RANDOM("Random");
}