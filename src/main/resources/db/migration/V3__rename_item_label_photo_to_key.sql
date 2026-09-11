-- The label photo is now an object-storage reference, not a client-supplied URL.
ALTER TABLE shopping_session_item RENAME COLUMN label_photo_url TO label_photo_key;
