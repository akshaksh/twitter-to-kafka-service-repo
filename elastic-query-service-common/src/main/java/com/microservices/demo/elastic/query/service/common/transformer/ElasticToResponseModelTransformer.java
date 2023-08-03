package com.microservices.demo.elastic.query.service.common.transformer;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ElasticToResponseModelTransformer {

  public ElasticQueryServiceResponseModel getResponseModel(TwitterIndexModel twitterIndexModel){
    return ElasticQueryServiceResponseModel
        .builder()
        .id(twitterIndexModel.getId())
        .userId(Long.valueOf(twitterIndexModel.getUserId()))
        .text(twitterIndexModel.getText())
        .createdAt(twitterIndexModel.getCreatedAt())
        .build();
  }

  public  List<ElasticQueryServiceResponseModel> getResponseModels(List<TwitterIndexModel> twitterIndexModel){
    return twitterIndexModel.stream().map(this::getResponseModel).collect(Collectors.toList());
  }

}
