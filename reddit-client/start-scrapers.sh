#!/usr/bin/env bash

# wait for kafka to start
sleep 1s

python ./comment-scraper.py &
python ./post-scraper.py &

wait -n