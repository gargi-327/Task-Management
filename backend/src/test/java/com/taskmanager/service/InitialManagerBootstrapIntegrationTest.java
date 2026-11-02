package com.taskmanager.service;

import com.taskmanager.entity.Role;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.bootstrap.manager.name=Bootstrap Manager",
        "app.bootstrap.manager.email=bootstrap@example.test",
        "spring.datasource.url=jdbc:h2:mem:bootstrap-manager-test;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class InitialManagerBootstrapIntegrationTest {
    @Autowired private InitialManagerBootstrap bootstrap;
    @Autowired private UserRepository users;

    @Test
    void startupCreatesAndRepeatedBootstrapKeepsOneManager() {
        assertEquals(1, users.count());
        assertEquals(Role.MANAGER,
                users.findByEmailIgnoreCase("bootstrap@example.test").orElseThrow().getRole());

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, users.count());
    }
}
