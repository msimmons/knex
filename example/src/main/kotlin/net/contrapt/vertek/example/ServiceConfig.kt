package net.contrapt.vertek.example

import net.contrapt.vertek.example.service.ResultService
import net.contrapt.vertek.example.service.SimpleService
import org.springframework.context.support.beans

object ServiceConfig {

    fun context() = beans {
        bean() { SimpleService.Impl() }
        bean() { ResultService.Impl() }

    }
}