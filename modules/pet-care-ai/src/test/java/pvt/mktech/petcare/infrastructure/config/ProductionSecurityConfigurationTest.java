package pvt.mktech.petcare.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSecurityConfigurationTest {

    @Test
    void disablesSecuritySensitiveDiagnosticsAndApiDocumentation() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application-prod.yml"));
        Properties properties = loader.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("sa-token.is-print")).isEqualTo("false");
        assertThat(properties.getProperty("sa-token.is-log")).isEqualTo("false");
        assertThat(properties.getProperty("knife4j.enable")).isEqualTo("false");
        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
    }
}
