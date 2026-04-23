'use strict';

function cleanText(v, fallback = '') {
  return typeof v === 'string' ? v : fallback;
}

function normalizeRole(role, isAdmin = false) {
  if (isAdmin) return 'admin';
  const value = cleanText(role, 'editor').trim().toLowerCase();
  return ['admin', 'editor', 'read', 'guest'].includes(value) ? value : 'editor';
}

function userRole(user) {
  return normalizeRole(user?.role, !!user?.is_admin);
}

function canWrite(user) {
  return ['admin', 'editor'].includes(userRole(user));
}

function publicUser(user) {
  return {
    id: user.id,
    name: user.name,
    username: user.username,
    role: userRole(user),
    is_admin: !!user.is_admin,
    created_at: user.created_at
  };
}

module.exports = { cleanText, normalizeRole, userRole, canWrite, publicUser };
