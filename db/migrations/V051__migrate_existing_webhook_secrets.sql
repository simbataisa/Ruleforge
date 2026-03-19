UPDATE webhooks
SET auth_type = 'SIGNING_SECRET'
WHERE secret IS NOT NULL
  AND auth_type = 'SIGNING_SECRET';

UPDATE webhooks
SET auth_type = 'NONE'
WHERE secret IS NULL
  AND auth_type = 'SIGNING_SECRET';
