package com.datadog.lambda.sample.java;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.datadoghq.datadog_lambda_java.DDLambda;
import datadog.trace.api.interceptor.MutableSpan;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.log4j.Logger;

public class Handler
  implements
    RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {

  private static final Logger LOG = Logger.getLogger(Handler.class);
  private static int invocationCount = 0;

  private void setSpanTags() {
    LOG.info("Setting a custom tag on the active span");

    // get active span
    final Span span = GlobalTracer.get().activeSpan();

    if (span != null && (span instanceof MutableSpan)) {
      // make sure span is mutable
      MutableSpan localRootSpan = ((MutableSpan) span).getLocalRootSpan();
      if (localRootSpan != null) {
        LOG.info("Local root span is NOT null");
        localRootSpan.setTag("foo", "bar");
      } else {
        LOG.info("Local root span IS null");
      }
    } else {
      LOG.info("Span is null");
    }
  }

  @Override
  public APIGatewayV2ProxyResponseEvent handleRequest(
    APIGatewayV2ProxyRequestEvent request,
    Context context
  ) {
    DDLambda li = new DDLambda(request, context); //Required to initialize the trace

    // user's business logic goes here

    li.metric("foo", 6.3, null);

    invocationCount++;

    // set a custom tag on the active span
    setSpanTags();

    li.metric("my_invocations", invocationCount, null);

    APIGatewayV2ProxyResponseEvent response = new APIGatewayV2ProxyResponseEvent();

    li.finish(); //Required to finish the active span.

    response.setStatusCode(200);
    return response;
  }
}
