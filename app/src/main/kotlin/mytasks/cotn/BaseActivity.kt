package mytasks.cotn

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase))
    }
}