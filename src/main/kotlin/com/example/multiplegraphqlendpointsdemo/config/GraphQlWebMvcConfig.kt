package com.example.multiplegraphqlendpointsdemo.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.server.WebGraphQlHandler
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler
import org.springframework.graphql.server.webmvc.GraphiQlHandler
import org.springframework.graphql.server.webmvc.SchemaHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.RequestPredicates
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class GraphQlWebMvcConfig {

    @Bean
    fun externalWebGraphQlHandler(
        externalExecutionGraphQlService: ExecutionGraphQlService,
        interceptors: ObjectProvider<WebGraphQlInterceptor>
    ): WebGraphQlHandler {
        return WebGraphQlHandler.builder(externalExecutionGraphQlService).interceptors(interceptors.orderedStream().toList()).build()
    }

    @Bean
    fun internalWebGraphQlHandler(
        internalExecutionGraphQlService: ExecutionGraphQlService,
        interceptors: ObjectProvider<WebGraphQlInterceptor>
    ): WebGraphQlHandler {
        return WebGraphQlHandler.builder(internalExecutionGraphQlService).interceptors(interceptors.orderedStream().toList()).build()
    }

    @Bean
    fun externalGraphQlHttpHandler(externalWebGraphQlHandler: WebGraphQlHandler): GraphQlHttpHandler {
        return GraphQlHttpHandler(externalWebGraphQlHandler)
    }

    @Bean
    fun internalGraphQlHttpHandler(internalWebGraphQlHandler: WebGraphQlHandler): GraphQlHttpHandler {
        return GraphQlHttpHandler(internalWebGraphQlHandler)
    }

    @Bean
    @Order(0)
    fun externalGraphQlRouterFunction(externalGraphQlHttpHandler: GraphQlHttpHandler, externalGraphQlSource: GraphQlSource, properties: GraphQlProperties): RouterFunction<ServerResponse> {
        val path = "/external-graphql"
        var builder = RouterFunctions.route()
        builder = builder.GET(path, this::onlyAllowPost)
        builder = builder.POST(path, RequestPredicates.contentType(MediaType.APPLICATION_JSON).and(RequestPredicates.accept(*SUPPORTED_MEDIA_TYPES)), externalGraphQlHttpHandler::handleRequest)
        if (properties.graphiql.isEnabled) {
            val graphiQlHandler = GraphiQlHandler(path, properties.websocket.path)
            builder = builder.GET(properties.graphiql.path, graphiQlHandler::handleRequest)
        }
        if (properties.schema.printer.isEnabled) {
            val schemaHandler = SchemaHandler(externalGraphQlSource)
            builder = builder.GET("$path/schema", schemaHandler::handleRequest)
        }
        return builder.build()
    }

    @Bean
    @Order(1)
    fun internalGraphQlRouterFunction(internalGraphQlHttpHandler: GraphQlHttpHandler, internalGraphQlSource: GraphQlSource, properties: GraphQlProperties): RouterFunction<ServerResponse> {
        val path = "/internal-graphql"
        var builder = RouterFunctions.route()
        builder = builder.GET(path, this::onlyAllowPost)
        builder = builder.POST(path, RequestPredicates.contentType(MediaType.APPLICATION_JSON).and(RequestPredicates.accept(*SUPPORTED_MEDIA_TYPES)), internalGraphQlHttpHandler::handleRequest)
        if (properties.graphiql.isEnabled) {
            val graphiQlHandler = GraphiQlHandler(path, properties.websocket.path)
            builder = builder.GET(properties.graphiql.path, graphiQlHandler::handleRequest)
        }
        if (properties.schema.printer.isEnabled) {
            val schemaHandler = SchemaHandler(internalGraphQlSource)
            builder = builder.GET("$path/schema", schemaHandler::handleRequest)
        }
        return builder.build()
    }

    private fun onlyAllowPost(@Suppress("UNUSED_PARAMETER") request: ServerRequest): ServerResponse {
        return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).headers(this::onlyAllowPost).build()
    }

    private fun onlyAllowPost(headers: HttpHeaders) {
        headers.allow = setOf(HttpMethod.POST)
    }

    companion object {
        private val SUPPORTED_MEDIA_TYPES = arrayOf(
            MediaType.APPLICATION_GRAPHQL_RESPONSE,
            MediaType.APPLICATION_JSON
        )
    }
}