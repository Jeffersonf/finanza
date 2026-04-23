'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeRole, userRole, canWrite, publicUser } = require('../server/permissions');

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
    created_at: undefined
  });
});
