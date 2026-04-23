'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const {
  moneyToNumber,
  inferRecurring,
  parseDateFromText,
  parseTransactionText
} = require('../server/transactionParser');

const baseDate = new Date('2026-04-22T12:00:00Z');

test('converte valores brasileiros e simples para numero', () => {
  assert.equal(moneyToNumber('87,90'), 87.90);
  assert.equal(moneyToNumber('1.234,56'), 1234.56);
  assert.equal(moneyToNumber('R$ 1.234,56'), 1234.56);
  assert.equal(moneyToNumber('350'), 350);
});

test('interpreta datas relativas e vencimento por dia do mes', () => {
  assert.equal(parseDateFromText('mercado hoje', baseDate), '2026-04-22');
  assert.equal(parseDateFromText('recebi ontem', baseDate), '2026-04-21');
  assert.equal(parseDateFromText('pagar amanha', baseDate), '2026-04-23');
  assert.equal(parseDateFromText('internet vence dia 10', baseDate), '2026-05-10');
  assert.equal(parseDateFromText('boleto daqui 5 dias', baseDate), '2026-04-27');
});

test('parseia despesa livre com conta sugerida', () => {
  const parsed = parseTransactionText('mercado 87,90 hoje nubank', { baseDate });

  assert.equal(parsed.type, 'expense');
  assert.equal(parsed.amount, 87.90);
  assert.equal(parsed.amountCents, 8790);
  assert.equal(parsed.description, 'Mercado');
  assert.equal(parsed.category, 'Alimentação');
  assert.equal(parsed.date, '2026-04-22');
  assert.equal(parsed.pending, false);
  assert.equal(parsed.accountHint, 'nubank');
});

test('parseia receita de freela de ontem', () => {
  const parsed = parseTransactionText('recebi 350 freela ontem', { baseDate });

  assert.equal(parsed.type, 'income');
  assert.equal(parsed.amount, 350);
  assert.equal(parsed.description, 'Freela');
  assert.equal(parsed.category, 'Freelance');
  assert.equal(parsed.date, '2026-04-21');
  assert.equal(parsed.pending, false);
});

test('parseia salario como receita e usa categoria correta', () => {
  const parsed = parseTransactionText('salario R$ 4.500,00 caiu hoje itau', { baseDate });

  assert.equal(parsed.type, 'income');
  assert.equal(parsed.amount, 4500);
  assert.equal(parsed.amountCents, 450000);
  assert.equal(parsed.description, 'Caiu');
  assert.equal(parsed.category, 'Sal\u00e1rio');
  assert.equal(parsed.accountHint, 'itau');
});

test('parseia vencimento futuro como pendente', () => {
  const parsed = parseTransactionText('internet 120 vence dia 10', { baseDate });

  assert.equal(parsed.type, 'expense');
  assert.equal(parsed.amount, 120);
  assert.equal(parsed.description, 'Internet');
  assert.equal(parsed.category, 'Moradia');
  assert.equal(parsed.date, '2026-05-10');
  assert.equal(parsed.pending, true);
});

test('marca despesa recorrente quando texto indica assinatura ou fixo', () => {
  assert.equal(inferRecurring('netflix 39,90 assinatura mensal'), true);
  assert.equal(inferRecurring('aluguel 1200 fixo'), true);
  assert.equal(inferRecurring('mercado 80 hoje'), false);

  const parsed = parseTransactionText('netflix 39,90 assinatura mensal nubank', { baseDate });
  assert.equal(parsed.category, 'Assinaturas');
  assert.equal(parsed.recurring, true);
  assert.equal(parsed.pending, false);
});

test('reconhece conta com nome composto e categorias de despesa', () => {
  const parsed = parseTransactionText('farmacia 42,35 mercado pago ontem', { baseDate });

  assert.equal(parsed.type, 'expense');
  assert.equal(parsed.description, 'Farmacia');
  assert.equal(parsed.category, 'Sa\u00fade');
  assert.equal(parsed.date, '2026-04-21');
  assert.equal(parsed.accountHint, 'mercado pago');
});

test('retorna null quando nao ha valor reconhecivel', () => {
  assert.equal(parseTransactionText('mercado hoje nubank', { baseDate }), null);
  assert.equal(parseTransactionText('mercado 0 hoje nubank', { baseDate }), null);
  assert.equal(parseTransactionText('', { baseDate }), null);
});
