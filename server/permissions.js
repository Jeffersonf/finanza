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

function isAdmin(user) {
  return !!user && userRole(user) === 'admin';
}

function canWrite(user) {
  if (!user) return false;
  return ['admin', 'editor'].includes(userRole(user));
}

function getAdminMutationError(actor, targetUserId, options = {}) {
  const action = cleanText(options.action, 'manage');
  const nextRole = normalizeRole(options.nextRole, false);

  if (!actor?.id || !targetUserId) return '';
  if (String(actor.id) !== String(targetUserId)) return '';

  if (action === 'delete') return 'Use outra conta admin para remover sua própria conta';
  if (action === 'role' && nextRole !== 'admin') {
    return 'Use outra conta admin para alterar seu próprio papel';
  }

  return '';
}

function publicUser(user = {}) {
  return {
    id: user.id,
    name: user.name,
    username: user.username,
    role: userRole(user),
    is_admin: !!user.is_admin,
    created_at: user.created_at
  };
}

module.exports = { cleanText, normalizeRole, userRole, isAdmin, canWrite, getAdminMutationError, publicUser };
