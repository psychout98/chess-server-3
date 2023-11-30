package com.example.chessserver3.integration.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

@AutoConfigureMockMvc
@WebMvcTest
class ChessControllerTest extends Specification {

    @Autowired
    private MockMvc mvc;

    def "get a user"() {
        expect: "Error no headers"
        mvc.perform(get("/user"))
        .andExpect(status().isOk())
    }
}
