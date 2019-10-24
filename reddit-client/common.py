from praw import Reddit
from praw.models import Redditor
from kafka import KafkaProducer
from kafka.errors import KafkaError
from time import sleep
from sys import stderr
import os
import json


def get_stuff_from_reddit_to_kafka(stuff_getter, stuff_whitelisted_keys, destination_topic):
    reddit = Reddit(client_id='BQ-GZCjz6VM2hQ', client_secret='UpYPBbMUG2X8_qq84cYDJzkDk-o',
                    user_agent='linux:dev.robakowski.id2221.finalproject:v0.0.1 (by /u/themicroworm)')

    kafka_url = os.environ.get('KAFKA_URL', 'localhost:9092')

    def start_kafka_client(max_attempts, url):
        attempt = 0
        while attempt < max_attempts:
            try:
                return KafkaProducer(bootstrap_servers=url)
            except KafkaError as e:
                if not e.retriable:
                    raise
                print(e, file=stderr)
                attempt += 1
                print(f'retrying kafka connection after {attempt} seconds', file=stderr)
                sleep(attempt)  # linear backoff

    kafka = start_kafka_client(10, kafka_url)

    filter_nsfw = True if os.environ.get('NSFW_FILTER') == '1' else False

    for stuff in stuff_getter(reddit):
        if filter_nsfw and stuff.over_18:
            continue
        # import pprint
        # pprint.pprint(vars(stuff))
        payload = json.dumps({
            key: value if not isinstance(value, Redditor) else value.name
            for key, value in vars(stuff).items()
            if key in stuff_whitelisted_keys
        })
        print(payload)
        kafka.send(destination_topic, payload.encode('utf8'))
