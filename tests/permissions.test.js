'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeRole, userRole, isAdmin, canWrite, getAdminMutationError, publicUser } = require('../server/permissions');

test('normaliza papeis conhecidos e usa editor como padrao seguro', () => {
  assert.equal(normalizeRole('admin'), 'admin');
  assert.equal(normalizeRole(' Editor '), 'editor');
  assert.equal(normalizeRole('read'), 'read');
  assert.equal(normalizeRole('guest'), 'guest');
  assert.equal(normalizeRole('qualquer-coisa'), 'editor');
  assert.equal(normalizeRole('', false), 'editor');
});

test('is_admin legado sempre prevalece como admin', () => {
  assert.equal(normalizeRole('guest', true), 'admin');
  assert.equal(userRole({ role: 'read', is_admin: true }), 'admin');
  assert.equal(isAdmin({ role: 'read', is_admin: true }), true);
  assert.equal(isAdmin({ role: 'editor', is_admin: false }), false);
});

test('somente admin e editor podem escrever', () => {
  assert.equal(canWrite({ role: 'admin' }), true);
  assert.equal(canWrite({ role: 'editor' }), true);
  assert.equal(canWrite({ role: 'read' }), false);
  assert.equal(canWrite({ role: 'guest' }), false);
  assert.equal(canWrite(null), false);
});

test('publicUser nao expõe api_key nem hash de senha', () => {
  const user = publicUser({
    id: 'u1',
    name: 'Jefferson',
    username: 'jefferson',
    role: 'admin',
    is_admin: true,
    created_at: '2026-04-22',
    api_key: 'segredo',
    password_hash: 'hash'
  });

  assert.deepEqual(user, {
    id: 'u1',
    name: 'Jefferson',
    username: 'jefferson',
    role: 'admin',
    is_admin: true,
    two_factor_enabled: false,
    created_at: '2026-04-22'
  });
});

test('publicUser aceita usuario ausente sem vazar dados', () => {
  assert.deepEqual(publicUser(), {
    id: undefined,
    name: undefined,
    username: undefined,
    role: 'editor',
    is_admin: false,
    two_factor_enabled: false,
    created_at: undefined
  });
});

test('bloqueia auto-rebaixamento e auto-remocao do admin autenticado', () => {
  assert.equal(
    getAdminMutationError({ id: 'u1', role: 'admin' }, 'u1', { action: 'role', nextRole: 'editor' }),
    'Use outra conta admin para alterar seu próprio papel'
  );
  assert.equal(
    getAdminMutationError({ id: 'u1', role: 'admin' }, 'u1', { action: 'delete' }),
    'Use outra conta admin para remover sua própria conta'
  );
  assert.equal(
    getAdminMutationError({ id: 'u1', role: 'admin' }, 'u1', { action: 'role', nextRole: 'admin' }),
    ''
  );
  assert.equal(
    getAdminMutationError({ id: 'u1', role: 'admin' }, 'u2', { action: 'role', nextRole: 'editor' }),
    ''
  );
});
