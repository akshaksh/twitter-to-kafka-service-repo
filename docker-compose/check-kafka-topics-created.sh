#!/bin/bash
# check-kafka-topics-created.sh

#run the update on system
apt-get update -y

#install kafkacat command utility
yes | apt-get install kafkacat

kafkacatResult=$(kafkacat -L -b kafka-broker-1:9092)

echo "kafkacat result:" $kafkacatResult

while [[ ! $kafkacatResult == *"twitter-topic"* ]]; do
  >&2 echo "Kafka topic has not been created yet!"
  sleep 2
  kafkacatResult=$(kafkacat -L -b kafka-broker-1:9092)
  echo "kafkacat result inside the loop for kafka topic check:" $kafkacatResult
done

echo "Kafka topic has been created successfully!!!!"

/cnb/process/web
