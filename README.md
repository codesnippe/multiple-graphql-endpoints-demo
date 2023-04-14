# multiple-graphql-endpoints-demo

This is a sample demo project that is built to have two separate graphQL endpoints.
One represents an external endpoint with a different/limited schema then the internal endpoint.

GraphiQL is enabled for easier testing, and after starting the app both of the endpoints can be found on:
* http://localhost:8080/graphiql?path=/external-graphql
* http://localhost:8080/graphiql?path=/internal-graphql
