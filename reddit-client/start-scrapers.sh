#!/usr/bin/env bash

# wait for kafka to start
sleep 5s

python ./comment-scraper.py &
python ./post-scraper.py