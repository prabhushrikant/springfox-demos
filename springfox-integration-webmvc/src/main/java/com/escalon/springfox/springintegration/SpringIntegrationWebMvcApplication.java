package com.escalon.springfox.springintegration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@SpringBootApplication
@RestController
public class SpringIntegrationWebMvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationWebMvcApplication.class, args);
    }

    ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("SpringFox demo API")
            .description("Api for converting to upper or lower.")
            .license("")
            .licenseUrl("http://unlicense.org")
            .version("1.0.0")
            .build();
    }

    @Bean
    public Docket customImplementation() {
        return new Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.escalon.springfox.springintegration"))
            .build()
            .useDefaultResponseMessages(false)
            .apiInfo(apiInfo());
    }

    @Bean
    public IntegrationFlow toUpperGetFlow() {
        return IntegrationFlows.from(
                Http.inboundGateway("/conversions/pathvariable/{upperLower}")
                        .requestMapping(r -> r
                                .methods(HttpMethod.GET)
                                .params("toConvert"))
                        .headerExpression("upperLower",
                                "#pathVariables.upperLower")
                        .payloadExpression("#requestParams['toConvert'][0]")
                        .id("toUpperLowerGateway"))
                .<String>handle((p, h) -> "upper".equals(h.get("upperLower")) ? p.toUpperCase() : p.toLowerCase())
                .get();
    }


    @Bean
    public IntegrationFlow toUpperFlow() {
        return IntegrationFlows.from(
                Http.inboundGateway("/conversions/upper")
                        .requestMapping(r -> r.methods(HttpMethod.POST)
                                .consumes("text/plain"))
                        .requestPayloadType(String.class)
                        .id("toUpperGateway"))
                .<String>handle((p, h) -> p.toUpperCase())
                .get();
    }

    @Bean
    public IntegrationFlow toLowerFlow() {
        return IntegrationFlows.from(
                Http.inboundGateway("/conversions/lower")
                        .requestMapping(r -> r.methods(HttpMethod.POST)
                                .consumes("application/json"))
                        .requestPayloadType(Foo.class)
                        .id("toLowerGateway"))
                .<Foo>handle((p, h) -> new Foo(p.getBar()
                        .toLowerCase()))
                .get();
    }

    @ApiResponses(
            @ApiResponse(code = 200,
                message = "OK",
                response = Baz.class,
                examples = @Example(@ExampleProperty(mediaType = "application/json",
                    value = "{'gnarf':'dragons'}"))
            )
    )
    @PostMapping("/conversion/controller")
    @Operation(
        summary = "Submit request to convert.",
        description = "Submit request to convert to lower or upper.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Returns the converted results.",
                content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = Baz.class))
                }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{'errorCode':'400', 'message': 'Bad input request.'}"),
                        schema = @Schema(implementation = ErrorResponse.class))
                }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "If service encountered errors communicating with other services that it depends on",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{'errorCode':'500', 'message': 'Internal server error.'}"),
                        schema = @Schema(implementation = ErrorResponse.class))
                }
            )}
    )
    public ResponseEntity<Baz> convert(@RequestBody Baz baz) {
        return new ResponseEntity(baz, HttpStatus.OK);
    }

    /** Home redirection to swagger api documentation */
    @Controller
    @ApiIgnore
    public class HomeController {
        @RequestMapping(value = "/")
        public String index() {
            System.out.println("/swagger-ui/index.html");
            return "redirect:/swagger-ui/index.html";
        }
    }

    public static class Baz {
        public String getGnarf() {
            return gnarf;
        }

        public void setGnarf(String gnarf) {
            this.gnarf = gnarf;
        }

        @ApiModelProperty(value = "gnarf variable", example = "useless")
        private String gnarf;
    }

    public static class Foo {
        private String bar;
        private boolean foo = false;
        private int count = 3;

        public Foo() {

        }

        public Foo(String bar) {
            this.bar = bar;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        public boolean isFoo() {
            return foo;
        }

        public void setFoo(boolean foo) {
            this.foo = foo;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    /** Describe the error message and code */
    @ApiModel(description = "Describe the error message and code")
    @Validated
    public static class ErrorResponse {

        @ApiModelProperty(required = true, value = "Different Error Conditions")
        @JsonProperty("errorCode")
        public String errorCode = null;

        @ApiModelProperty(required = true, value = "Provides description of error")
        @JsonProperty("message")
        public String message = null;

        public ErrorResponse errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
    }
}
