package io.github.reladomokotlin.demo.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource
import java.io.InputStream

@Configuration
class ReladomoConfig {

    @Autowired
    private lateinit var dataSource: DataSource

    @Bean
    fun mithraManager(): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(120)

        // Load Reladomo configuration from XML
        val configStream: InputStream = javaClass.classLoader.getResourceAsStream("MithraRuntimeConfig.xml")
            ?: throw IllegalStateException("Could not find MithraRuntimeConfig.xml")

        manager.readConfiguration(configStream)
        manager.fullyInitialize()

        return manager
    }
}
