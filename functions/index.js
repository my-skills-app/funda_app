const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onValueCreated } = require('firebase-functions/v2/database');
const { setGlobalOptions } = require('firebase-functions/v2');
const admin = require('firebase-admin');

admin.initializeApp();

setGlobalOptions({ region: 'asia-southeast1' });

const TOPIC_DEFAULT = 'all_users';
const CHANNEL_ID = 'app_push_hidden';

async function sendTopicPush(title, body, topic, imageUrl, linkUrl) {
  const data = { title, body };
  if (imageUrl) {
    data.imageUrl = imageUrl;
  }
  if (linkUrl) {
    data.linkUrl = linkUrl;
  }
  const notification = { title, body };
  if (imageUrl) {
    notification.imageUrl = imageUrl;
  }
  return admin.messaging().send({
    notification,
    data,
    topic: topic || TOPIC_DEFAULT,
    android: {
      priority: 'high',
      notification: { channelId: CHANNEL_ID },
    },
  });
}

exports.sendPushBroadcast = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Sign in required');
  }
  const title = String(request.data?.title || '').trim();
  const body = String(request.data?.body || '').trim();
  const topic = String(request.data?.topic || TOPIC_DEFAULT).trim();
  if (!title || !body) {
    throw new HttpsError('invalid-argument', 'Title and message are required');
  }
  const messageId = await sendTopicPush(title, body, topic);
  return { messageId };
});

exports.onPushBroadcastQueued = onValueCreated(
  {
    ref: '/push_broadcasts/{pushId}',
    region: 'asia-southeast1',
    instance: 'bhagwat-geeta-f7087-default-rtdb',
  },
  async (event) => {
    const data = event.data.val();
    if (!data || data.status === 'sent') {
      return;
    }
    const title = String(data.title || '').trim();
    const body = String(data.body || '').trim();
    if (!title || !body) {
      await event.data.ref.update({
        status: 'failed',
        error: 'Missing title or body',
        failedAt: Date.now(),
      });
      return;
    }
    try {
      const imageUrl = String(data.imageUrl || '').trim() || undefined;
      const linkUrl = String(data.linkUrl || '').trim() || undefined;
      const messageId = await sendTopicPush(title, body, data.topic, imageUrl, linkUrl);
      await event.data.ref.update({
        status: 'sent',
        messageId,
        sentAt: Date.now(),
      });
    } catch (err) {
      await event.data.ref.update({
        status: 'failed',
        error: err.message || 'Send failed',
        failedAt: Date.now(),
      });
    }
  }
);
