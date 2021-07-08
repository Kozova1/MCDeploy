import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.getOrElse
import net.vogman.mcdeploy.Config
import net.vogman.mcdeploy.DEFAULT_CONFIG
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CheckDefaultConfig {
    @Test
    fun testDefaultConfig() {
        val defaultConfig = ConfigLoader.Builder()
            .addSource(PropertySource.string(DEFAULT_CONFIG, "toml"))
            .build()
            .loadConfig<Config>()
            .getOrElse {
                println(it.description())
                assertEquals(1, 2)
            }
        println(defaultConfig)
    }
}