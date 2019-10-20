from common import get_stuff_from_reddit_to_kafka

get_stuff_from_reddit_to_kafka(
    stuff_getter=lambda reddit: reddit.subreddit('all').stream.comments(),
    stuff_whitelisted_keys=[
        'body_html',
        'body',
        'subreddit_name_prefixed',
        'permalink',
        'link_permalink',
        'link_author',
        'created_utc',
        'author',
        'over_18'
    ],
    destination_topic='comments'
)
