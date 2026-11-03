package com.minimal.carlauncher.service

import android.service.notification.NotificationListenerService

/**
 * Empty notification listener. Android only lets an app read the music
 * players' "now playing" info and send Play/Pause/Next to them after the user
 * turns on Notification Access for an app that has a listener like this one.
 * The launcher does not read or show any notifications.
 */
class MediaListenerService : NotificationListenerService()
