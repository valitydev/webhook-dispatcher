package dev.vality.webhook.dispatcher.dao;

import com.rbkmoney.easyway.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.FailureDetectingExternalResource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.function.Consumer;

@Slf4j
@DirtiesContext
@RunWith(SpringRunner.class)
@EnableConfigurationProperties({DataSourceProperties.class})
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class}, initializers = DaoTestBase.Initializer.class)
public abstract class DaoTestBase extends AbstractTestUtils {

    private static final TestContainers POSTGRES =
            TestContainersBuilder.builderWithTestContainers(TestContainersParameters::new)
                    .addPostgresqlTestContainer()
                    .build();

    @ClassRule
    public static final FailureDetectingExternalResource resource = new FailureDetectingExternalResource() {

        @Override
        protected void starting(Description description) {
            POSTGRES.startTestContainers();
        }

        @Override
        protected void failed(Throwable e, Description description) {
            log.warn("Test Container start failed ", e);
        }

        @Override
        protected void finished(Description description) {
            POSTGRES.stopTestContainers();
        }
    };

    private static Consumer<EnvironmentProperties> getEnvironmentPropertiesConsumer() {
        return environmentProperties -> {
            PostgreSQLContainer postgreSqlContainer = POSTGRES.getPostgresqlTestContainer().get();
            environmentProperties.put("spring.datasource.url", postgreSqlContainer.getJdbcUrl());
            environmentProperties.put("spring.datasource.username", postgreSqlContainer.getUsername());
            environmentProperties.put("spring.datasource.password", postgreSqlContainer.getPassword());
            environmentProperties.put("spring.flyway.url", postgreSqlContainer.getJdbcUrl());
            environmentProperties.put("spring.flyway.user", postgreSqlContainer.getUsername());
            environmentProperties.put("spring.flyway.password", postgreSqlContainer.getPassword());
        };
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues
                    .of(POSTGRES.getEnvironmentProperties(getEnvironmentPropertiesConsumer()))
                    .applyTo(configurableApplicationContext);
        }
    }
}
