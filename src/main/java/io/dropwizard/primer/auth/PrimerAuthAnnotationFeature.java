package io.dropwizard.primer.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.primer.auth.annotation.AuthWhitelist;
import io.dropwizard.primer.auth.annotation.Authorize;
import io.dropwizard.primer.auth.authorizer.PrimerAnnotationAuthorizer;
import io.dropwizard.primer.auth.filter.PrimerAuthAnnotationFilter;
import io.dropwizard.primer.model.PrimerBundleConfiguration;
import lombok.Builder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

@Provider
public class PrimerAuthAnnotationFeature implements DynamicFeature {

    @Context
    private HttpServletRequest requestProxy;

    private final PrimerBundleConfiguration configuration;
    private final ObjectMapper mapper;
    private final PrimerAnnotationAuthorizer authorizer;

    @Builder
    public PrimerAuthAnnotationFeature(final PrimerBundleConfiguration configuration, final ObjectMapper mapper, final PrimerAnnotationAuthorizer authorizer) {
        this.configuration = configuration;
        this.mapper = mapper;
        this.authorizer = authorizer;
    }

    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        Optional
                .ofNullable(resourceInfo.getResourceMethod())
                .map(resourceMethod -> resourceMethod.getAnnotation(Authorize.class))
                .ifPresent(authorize ->
                        featureContext.register(
                                PrimerAuthAnnotationFilter.builder()
                                        .requestProxy(requestProxy)
                                        .configuration(configuration)
                                        .objectMapper(mapper)
                                        .authorize(authorize)
                                        .authorizer(authorizer)
                                        .authWhitelist(resourceInfo.getResourceMethod().getAnnotation(AuthWhitelist.class))
                                        .build()
                        )
                );
    }
}