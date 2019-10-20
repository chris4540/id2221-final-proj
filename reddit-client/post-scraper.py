from common import get_stuff_from_reddit_to_kafka

get_stuff_from_reddit_to_kafka(
    stuff_getter=lambda reddit: reddit.subreddit('all').stream.submissions(),
    stuff_whitelisted_keys=[
        'title',
        'selftext',
        'selftext_html',
        'subreddit_name_prefixed',
        'permalink',
        'url',
        'created_utc',
        'author',
        'is_crosspostable',
        'is_meta',
        'is_original_content',
        'is_reddit_media_domain',
        'is_robot_indexable',
        'is_self',
        'is_video',
        'over_18',
        'link_flair_text',
        'domain',
        'category',
        'author_flair_text',
        'crosspost_parent',
        'post_hint',
        'parent_whitelist_status'
    ],
    destination_topic='posts'
)
