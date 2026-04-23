'use strict';

const { cleanText } = require('./permissions');

function normalizeText(value) {
  return cleanText(value)
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

function moneyToNumber(raw) {
  const v = cleanText(raw).replace(/r\$/gi, '').trim();
  if (!v) return 0;
  const normalized = v.includes(',') ? v.replace(/\./g, '').replace(',', '.') : v.replace(/\s+/g, '');
  return Number(normalized) || 0;
}

function inferCategoryFromText(text, type) {
  const normalized = normalizeText(text);
  if (type === 'income') {
    if (/(salario|pagamento|renda)/.test(normalized)) return 'Sal\u00e1rio';
    if (/(freela|freelance|job|cliente)/.test(normalized)) return 'Freelance';
    if (/(invest|rendimento)/.test(normalized)) return 'Investimentos';
    return 'Outros';
  }
  if (/(farmacia|remedio|medico|consulta|exame|saude)/.test(normalized)) return 'Sa\u00fade';
  if (/(mercado|supermercado|ifood|restaurante|lanche|padaria|comida|delivery)/.test(normalized)) return 'Alimenta\u00e7\u00e3o';
  if (/(uber|99|taxi|gasolina|combustivel|onibus|metro)/.test(normalized)) return 'Transporte';
  if (/(aluguel|luz|agua|internet|condominio|casa|energia)/.test(normalized)) return 'Moradia';
  if (/(curso|livro|faculdade|educacao)/.test(normalized)) return 'Educa\u00e7\u00e3o';
  if (/(cinema|bar|jogo|show|lazer)/.test(normalized)) return 'Lazer';
  if (/(netflix|spotify|prime|assinatura)/.test(normalized)) return 'Assinaturas';
  return 'Outros';
}

function dateOnly(date) {
  return date.toISOString().slice(0, 10);
}

function addDays(baseDate, days) {
  const date = new Date(baseDate);
  date.setDate(date.getDate() + days);
  return date;
}

function parseDateFromText(text, baseDate = new Date()) {
  const normalized = normalizeText(text);
  if (normalized.includes('ontem')) return dateOnly(addDays(baseDate, -1));
  if (normalized.includes('amanha')) return dateOnly(addDays(baseDate, 1));

  const inDays = normalized.match(/\b(?:em|daqui)\s+(\d{1,2})\s+dias?\b/);
  if (inDays) return dateOnly(addDays(baseDate, Number(inDays[1])));

  const dayMatch = normalized.match(/\b(?:dia|vence(?:\s+dia)?|vencimento(?:\s+dia)?)\s+(\d{1,2})\b/);
  if (dayMatch) {
    const day = Math.min(Math.max(Number(dayMatch[1]), 1), 31);
    const date = new Date(baseDate);
    date.setHours(12, 0, 0, 0);
    date.setDate(day);
    if (dateOnly(date) < dateOnly(baseDate)) date.setMonth(date.getMonth() + 1);
    return dateOnly(date);
  }

  return dateOnly(baseDate);
}

function inferAccountHint(text) {
  const normalized = normalizeText(text);
  const known = ['nubank', 'itau', 'bradesco', 'santander', 'inter', 'caixa', 'picpay', 'mercado pago'];
  return known.find(name => new RegExp(`\\b${name.replace(/\s+/g, '\\s+')}\\b`).test(normalized)) || '';
}

function inferRecurring(text) {
  return /\b(mensal|todo mes|todo mês|recorrente|fixo|fixa|assinatura)\b/.test(normalizeText(text));
}

function titleFromText(text, amountToken, fallback, accountHint = '') {
  let clean = cleanText(text)
    .replace(amountToken, ' ')
    .replace(/\b(?:r\$|gastei|paguei|comprei|recebi|receita|despesa|sal[a\u00e1]rio|entrada|ganhei|hoje|ontem|amanh[a\u00e3]|vence|vencimento|dia|em|daqui|dias?|no|na|de|com|pelo|pela)\b/gi, ' ')
    .replace(/\d{1,2}/g, ' ');

  if (accountHint) clean = clean.replace(new RegExp(accountHint.replace(/\s+/g, '\\s+'), 'ig'), ' ');

  clean = clean.replace(/\s+/g, ' ').trim();
  if (!clean) return fallback;
  return clean.charAt(0).toUpperCase() + clean.slice(1);
}

function parseTransactionText(text, options = {}) {
  const raw = cleanText(text).trim();
  if (!raw) return null;

  const amountMatch = raw.match(/(?:r\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{1,2}|\d+(?:[.,]\d{1,2})?)/i);
  if (!amountMatch) return null;

  const amountToken = amountMatch[0];
  const amount = moneyToNumber(amountMatch[1]);
  if (!amount) return null;

  const normalized = normalizeText(raw);
  const type = /(recebi|receita|salario|pix recebido|entrada|ganhei|freela|freelance)/.test(normalized) ? 'income' : 'expense';
  const date = parseDateFromText(raw, options.baseDate || new Date());
  const pending = type === 'expense' && (/(vence|vencimento|a pagar|boleto|conta fixa|fixo|fixa)/.test(normalized) || date > dateOnly(options.baseDate || new Date()));
  const category = inferCategoryFromText(raw, type);
  const accountHint = inferAccountHint(raw);
  const recurring = type === 'expense' && inferRecurring(raw);
  const description = titleFromText(raw, amountToken, category, accountHint);

  return {
    type,
    amount,
    amountCents: Math.round(amount * 100),
    description,
    category,
    date,
    pending,
    accountHint,
    recurring
  };
}

module.exports = {
  moneyToNumber,
  inferCategoryFromText,
  parseDateFromText,
  inferAccountHint,
  inferRecurring,
  parseTransactionText
};
