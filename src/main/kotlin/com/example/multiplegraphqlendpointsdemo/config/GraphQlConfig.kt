package com.example.multiplegraphqlendpointsdemo.config

import graphql.execution.instrumentation.Instrumentation
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring.Builder
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility
import java.io.IOException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.graphql.execution.DataFetcherExceptionResolver
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.graphql.execution.SubscriptionExceptionResolver

@Configuration
class GraphQlConfig {

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer {
                wiringBuilder: Builder ->
            wiringBuilder
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.GraphQLLong)
        }
    }

    @Bean
    fun externalGraphQlSource(
        resourcePatternResolver: ResourcePatternResolver,
        properties: GraphQlProperties,
        exceptionResolvers: ObjectProvider<DataFetcherExceptionResolver>,
        subscriptionExceptionResolvers: ObjectProvider<SubscriptionExceptionResolver>,
        instrumentations: ObjectProvider<Instrumentation>,
        wiringConfigurers: ObjectProvider<RuntimeWiringConfigurer>,
        sourceCustomizers: ObjectProvider<GraphQlSourceBuilderCustomizer>
    ): GraphQlSource {
        val schemaLocations = arrayOf("classpath:graphql/external/**/")
        val schemaResources = resolveSchemaResources(resourcePatternResolver, schemaLocations, properties.schema.fileExtensions)
        val builder = setupGraphQlSourceBuilder(schemaResources, exceptionResolvers, subscriptionExceptionResolvers, instrumentations, properties, wiringConfigurers, sourceCustomizers)
        return builder.build()
    }

    @Bean
    fun externalExecutionGraphQlService(externalGraphQlSource: GraphQlSource, batchLoaderRegistry: BatchLoaderRegistry): ExecutionGraphQlService {
        val externalService = DefaultExecutionGraphQlService(externalGraphQlSource)
        externalService.addDataLoaderRegistrar(batchLoaderRegistry)
        return externalService
    }

    @Bean
    fun internalExecutionGraphQlService(internalGraphQlSource: GraphQlSource, batchLoaderRegistry: BatchLoaderRegistry): ExecutionGraphQlService {
        val internalService = DefaultExecutionGraphQlService(internalGraphQlSource)
        internalService.addDataLoaderRegistrar(batchLoaderRegistry)
        return internalService
    }

    @Bean
    fun internalGraphQlSource(
        resourcePatternResolver: ResourcePatternResolver,
        properties: GraphQlProperties,
        exceptionResolvers: ObjectProvider<DataFetcherExceptionResolver>,
        subscriptionExceptionResolvers: ObjectProvider<SubscriptionExceptionResolver>,
        instrumentations: ObjectProvider<Instrumentation>,
        wiringConfigurers: ObjectProvider<RuntimeWiringConfigurer>,
        sourceCustomizers: ObjectProvider<GraphQlSourceBuilderCustomizer>
    ): GraphQlSource {
        val schemaLocations = arrayOf("classpath:graphql/internal/**/")
        val schemaResources = resolveSchemaResources(resourcePatternResolver, schemaLocations, properties.schema.fileExtensions)
        val builder = setupGraphQlSourceBuilder(schemaResources, exceptionResolvers, subscriptionExceptionResolvers, instrumentations, properties, wiringConfigurers, sourceCustomizers)
        return builder.build()
    }

    private fun setupGraphQlSourceBuilder(
        schemaResources: Array<Resource>,
        exceptionResolvers: ObjectProvider<DataFetcherExceptionResolver>,
        subscriptionExceptionResolvers: ObjectProvider<SubscriptionExceptionResolver>,
        instrumentations: ObjectProvider<Instrumentation>,
        properties: GraphQlProperties,
        wiringConfigurers: ObjectProvider<RuntimeWiringConfigurer>,
        sourceCustomizers: ObjectProvider<GraphQlSourceBuilderCustomizer>
    ): GraphQlSource.SchemaResourceBuilder {
        val builder = GraphQlSource.schemaResourceBuilder()
            .schemaResources(*schemaResources)
            .exceptionResolvers(exceptionResolvers.orderedStream().toList())
            .subscriptionExceptionResolvers(subscriptionExceptionResolvers.orderedStream().toList())
            .instrumentation(instrumentations.orderedStream().toList())
        if (!properties.schema.introspection.isEnabled) {
            builder.configureRuntimeWiring(this::enableIntrospection);
        }
        wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring)
        sourceCustomizers.orderedStream().forEach { customizer -> customizer.customize(builder) }
        return builder
    }

    private fun enableIntrospection(wiringBuilder: Builder): Builder {
        return wiringBuilder.fieldVisibility(NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY)
    }

    private fun resolveSchemaResources(resolver: ResourcePatternResolver, locations: Array<String>, extensions: Array<String>): Array<Resource> {
        val resources = mutableListOf<Resource>()
        locations.forEach { location ->
            extensions.forEach { extension ->
                resources.addAll(resolveSchemaResources(resolver, "$location*$extension"))
            }
        }
        return resources.toTypedArray()
    }

    private fun resolveSchemaResources(resolver: ResourcePatternResolver, pattern: String): List<Resource> {
        return try {
            resolver.getResources(pattern).toList()
        } catch (ex: IOException) {
            emptyList()
        }
    }
}
