package com.microservices.demo.reactive.elastic.query.service.business.impl;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.common.transformer.ElasticToResponseModelTransformer;
import com.microservices.demo.reactive.elastic.query.service.ElasticQueryService;
import com.microservices.demo.reactive.elastic.query.service.business.ReactiveElasticQueryClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
public class TwitterElasticQueryService implements ElasticQueryService {

  private static final Logger LOG= LoggerFactory.getLogger(TwitterElasticQueryService.class);
  private final ReactiveElasticQueryClient<TwitterIndexModel> reactiveElasticQueryClient;
  private final ElasticToResponseModelTransformer elasticToResponseModelTransformer;

  @Override
  public Flux<ElasticQueryServiceResponseModel> getDocumentByText(String text) {
    LOG.info("Querying reactive elasticsearch for text");
    return reactiveElasticQueryClient.getIndexModelByText(text).map(elasticToResponseModelTransformer::getResponseModel);
  }
}
