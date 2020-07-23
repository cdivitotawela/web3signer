/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.eth2signer.core.service.http.handlers;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.eth2signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;

import tech.pegasys.eth2signer.core.service.http.models.SigningRequestBody;
import tech.pegasys.eth2signer.core.service.operations.SignerForIdentifier;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignForIdentifierHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;

  public SignForIdentifierHandler(final SignerForIdentifier<?> signerForIdentifier) {
    this.signerForIdentifier = signerForIdentifier;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final RequestParameters params = routingContext.get("parsedParameters");
    final String identifier = params.pathParameter("identifier").toString();
    final String dataToSign = getDataToSign(params);

    final Optional<String> signature;
    try {
      signature = signerForIdentifier.sign(identifier, dataToSign);
    } catch (final IllegalArgumentException e) {
      routingContext.fail(400);
      return;
    }

    if (signature.isEmpty()) {
      LOG.trace("Unsuitable handler for {}, invoking next handler", identifier);
      routingContext.next();
    } else {
      routingContext.response().putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8).end(signature.get());
    }
  }

  private String getDataToSign(final RequestParameters params) {
    final RequestParameter body = params.body();
    final JsonObject jsonObject = body.getJsonObject();
    // the mapping shouldn't fail as openapifilter would have already thrown error on schema
    // mismatch
    return jsonObject.mapTo(SigningRequestBody.class).getData();
  }
}