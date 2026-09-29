////package com.example.demo.config;
////
////import io.swagger.v3.oas.models.OpenAPI;
////import io.swagger.v3.oas.models.info.Info;
////import io.swagger.v3.oas.models.security.SecurityRequirement;
////import io.swagger.v3.oas.models.security.SecurityScheme;
////import io.swagger.v3.oas.models.security.SecurityScheme.In;
////import io.swagger.v3.oas.models.security.SecurityScheme.Type;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////
////@Configuration
////public class SwaggerConfig {
////
////    @Bean
////    public OpenAPI customOpenAPI() {
////        return new OpenAPI()
////                .info(new Info()
////                        .title("API Documentation")
////                        .version("1.0.0")
////                        .description("API documentation for the application"))
////                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
////                .components(new io.swagger.v3.oas.models.Components()
////                        .addSecuritySchemes("Bearer Authentication",
////                                new SecurityScheme()
////                                        .type(Type.HTTP)
////                                        .scheme("bearer")
////                                        .bearerFormat("JWT")
////                                        .in(In.HEADER)
////                                        .name("Authorization")));
////    }
////}
//package com.example.demo.config;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration/
//public class SwaggerConfig {
//
//    @Bean
//    public OpenAPI customOpenAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("API Documentation")
//                        .version("1.0.0")
//                        .description("API documentation for the application"));
//    }
//}
