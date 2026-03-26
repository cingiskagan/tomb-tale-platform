package com.tombtale.servicecommerce.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestControllerTest {

    @Test
    void shouldReturnOkAndGreetingString() {
        TestController controller = new TestController();
        String response = controller.returnHelloWorld();
        assertThat(response).isEqualTo("Hello world.");
    }
}
