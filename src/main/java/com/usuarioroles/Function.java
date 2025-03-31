package com.usuarioroles;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private static final String ERROR_MESSAGE = "Please pass a name on the query string or in the request body";
    private static final String SUCCESS_TEMPLATE = "Hello, %s";

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Java HTTP trigger processed a request.");
        
        // Parse query parameter more efficiently
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);
        
        // Use builder pattern more efficiently
        return name == null
            ? request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                     .body(ERROR_MESSAGE)
                     .build()
            : request.createResponseBuilder(HttpStatus.OK)
                     .body(String.format(SUCCESS_TEMPLATE, name))
                     .build();
    }
}
