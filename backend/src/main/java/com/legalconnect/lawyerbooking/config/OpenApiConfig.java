package com.legalconnect.lawyerbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lawyerBookingOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.legalconnect.com");
        prodServer.setDescription("Production server");

        Contact contact = new Contact();
        contact.setEmail("support@legalconnect.com");
        contact.setName("LegalConnect Support");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Lawyer Booking System API")
                .version("1.0.0")
                .contact(contact)
                .description("API documentation for the Lawyer Booking System. " +
                        "This system allows users to upload audio recordings, which are processed with AI " +
                        "to transcribe, mask personal information, and generate multilingual audio. " +
                        "Lawyers can manage cases, communicate with users, and handle appointments.")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer));
    }
}
