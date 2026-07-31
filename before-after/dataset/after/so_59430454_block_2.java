  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .components(new Components().addSecuritySchemes("bearer-jwt",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER).name("Authorization")))
            .info(new Info().title("App API").version("snapshot"))
            .addSecurityItem(
                    new SecurityRequirement().addList("bearer-jwt", Arrays.asList("read", "write")));
  }
Global security schema can be overridden by a different one with the `@SecurityRequirements` annotation. Including removing security schemas for an operation. For example, we can remove security for registration path.
@SecurityRequirements
@PostMapping("/registration")
public ResponseEntity post(@RequestBody @Valid Registration: registration) {
    return registrationService.register(registration);
}
While still keeping security schemas for other APIs.
**Old answer (Dec 20 '19):**
Global security schema can be overridden by a different one with the `@SecurityRequirements` annotation. but it cannot be removed for unsecured  paths. It is acctualy missing fueature in the **springdoc-openapi**, OpenAPI standard allows it. See [disable global security for particular operation][1]
There is a workaround though. The **springdoc-openapi** has a concept of an OpenApiCustomiser which can be used to intercept generated schema. Inside the customizer, an operation can be modified programmatically. To remove any inherited security, the field `security` needs to be set to an empty array. The logic may be based on any arbitrary rules e.g operation name. I used tags.
The customizer:
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.api.OpenApiCustomiser;
import org.springframework.stereotype.Component;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Component
public class SecurityOverrideCustomizer implements OpenApiCustomiser {
    public static final String UNSECURED = "security.open";
    private static final List<Function<PathItem, Operation>> OPERATION_GETTERS = Arrays.asList(
            PathItem::getGet, PathItem::getPost, PathItem::getDelete, PathItem::getHead,
            PathItem::getOptions, PathItem::getPatch, PathItem::getPut);
    @Override
    public void customise(OpenAPI openApi) {
        openApi.getPaths().forEach((path, item) -> getOperations(item).forEach(operation -> {
            List<String> tags = operation.getTags();
            if (tags != null && tags.contains(UNSECURED)) {
                operation.setSecurity(Collections.emptyList());
                operation.setTags(filterTags(tags));
            }
        }));
    }
    private static Stream<Operation> getOperations(PathItem pathItem) {
        return OPERATION_GETTERS.stream()
                .map(getter -> getter.apply(pathItem))
                .filter(Objects::nonNull);
    }
    private static List<String> filterTags(List<String> tags) {
        return tags.stream()
                .filter(t -> !t.equals(UNSECURED))
                .collect(Collectors.toList());
    }
}
Now we can add `@Tag(name = SecurityOverrideCustomizer.UNSECURED)` to unsecured methods:
    @Tag(name = SecurityOverrideCustomizer.UNSECURED)
    @GetMapping("/open")
    @ResponseBody
    public String open() {
        return "It works!";
    }
Please bear in mind that it is just a workaround. Hopefully, the issue will be resolved in the next **springdoc-openapi** versions (at the time of writing it the current version is 1.2.18).
For a working example see [springdoc-security-override-fix][2]
  [1]: https://github.com/swagger-api/swagger-core/issues/2844
  [2]: https://github.com/mafor/springdoc-security-override-fix