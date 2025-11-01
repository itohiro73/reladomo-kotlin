package io.github.chronostaff.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.InputStream
import javax.sql.DataSource

@Configuration
class ReladomoConfig {

    @Autowired
    private lateinit var dataSource: DataSource

    @Bean
    fun mithraManager(): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(120)

        val configStream: InputStream = javaClass.classLoader.getResourceAsStream("MithraRuntimeConfig.xml")
            ?: throw IllegalStateException("Could not find MithraRuntimeConfig.xml")

        manager.readConfiguration(configStream)
        manager.fullyInitialize()

        return manager
    }
}
