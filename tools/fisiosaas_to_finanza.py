#!/usr/bin/env python3
"""
Convert a FisioSaaS/agenda.db SQLite database into a Finanza backup JSON.

The converter intentionally migrates only financial data:
- paid/pending appointments become income transactions
- registered expenses become expense transactions

Clinical records, documents, anamneses and patient notes stay out of Finanza.
"""

from __future__ import annotations

import argparse
import json
import os
import sqlite3
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path
from typing import Any


DEFAULT_SOURCE = Path(r"C:\Users\jeffe\Downloads\BarbeariaSaaS\agenda.db")
DEFAULT_ACCOUNT_ID = "fisiosaas-caixa"


def now_slug() -> str:
    return datetime.now().strftime("%Y%m%d-%H%M%S")


def row_dict(row: sqlite3.Row) -> dict[str, Any]:
    return {key: row[key] for key in row.keys()}


def clean_text(value: Any, fallback: str = "") -> str:
    if value is None:
        return fallback
    text = str(value).strip()
    return text or fallback


def money(value: Any) -> float:
    if value is None or value == "":
        return 0.0
    if isinstance(value, str):
        value = value.strip().replace(".", "").replace(",", ".") if "," in value else value
    try:
        return round(float(value), 2)
    except (TypeError, ValueError):
        return 0.0


def iso_date(value: Any, fallback: str | None = None) -> str:
    text = clean_text(value)
    if not text:
        return fallback or datetime.now().date().isoformat()

    candidates = [
        "%Y-%m-%d",
        "%d/%m/%Y",
        "%d/%m/%Y %H:%M",
        "%Y-%m-%d %H:%M:%S",
    ]
    for fmt in candidates:
        try:
            return datetime.strptime(text[: len(fmt)], fmt).date().isoformat()
        except ValueError:
            pass

    return text[:10] if len(text) >= 10 else (fallback or datetime.now().date().isoformat())


def fetch_rows(conn: sqlite3.Connection, table: str) -> list[dict[str, Any]]:
    try:
        rows = conn.execute(f"SELECT * FROM {table}").fetchall()
    except sqlite3.Error:
        return []
    return [row_dict(row) for row in rows]


def appointment_to_tx(row: dict[str, Any]) -> dict[str, Any] | None:
    amount = money(row.get("valor_cobrado"))
    if amount <= 0:
        return None

    status = clean_text(row.get("status_financeiro"), "PENDENTE").upper()
    client = clean_text(row.get("cliente"), "Paciente")
    date = iso_date(row.get("data_pagamento") if status == "PAGO" else row.get("data"), iso_date(row.get("data")))
    source_id = row.get("id")
    paid = status == "PAGO"

    note_parts = [
        f"Origem: FisioSaaS agendamento #{source_id}",
        f"Status financeiro: {status}",
    ]
    if row.get("data"):
        note_parts.append(f"Data atendimento: {row.get('data')}")
    if row.get("horario"):
        note_parts.append(f"Horario: {row.get('horario')}")
    if row.get("forma_pagamento"):
        note_parts.append(f"Pagamento: {row.get('forma_pagamento')}")
    if row.get("status"):
        note_parts.append(f"Agenda: {row.get('status')}")

    return {
        "id": f"fisiosaas-agendamento-{source_id}",
        "type": "income",
        "desc": f"Sessao - {client}",
        "description": f"Sessao - {client}",
        "amount": amount,
        "category": "Atendimentos",
        "date": date,
        "note": " | ".join(note_parts),
        "accountId": DEFAULT_ACCOUNT_ID,
        "account_id": DEFAULT_ACCOUNT_ID,
        "paid": False,
        "pending": not paid,
    }


def expense_to_tx(row: dict[str, Any]) -> dict[str, Any] | None:
    amount = money(row.get("valor"))
    if amount <= 0:
        return None

    source_id = row.get("id")
    category = clean_text(row.get("categoria"), "FisioSaaS")
    description = clean_text(row.get("descricao"), "Despesa FisioSaaS")

    return {
        "id": f"fisiosaas-despesa-{source_id}",
        "type": "expense",
        "desc": description,
        "description": description,
        "amount": amount,
        "category": f"Negocio - {category}",
        "date": iso_date(row.get("data")),
        "note": f"Origem: FisioSaaS despesa #{source_id}",
        "accountId": DEFAULT_ACCOUNT_ID,
        "account_id": DEFAULT_ACCOUNT_ID,
        "paid": False,
        "pending": False,
    }


def build_backup(source: Path) -> dict[str, Any]:
    if not source.exists():
        raise FileNotFoundError(f"SQLite nao encontrado: {source}")

    conn = sqlite3.connect(source)
    conn.row_factory = sqlite3.Row
    try:
        appointments = fetch_rows(conn, "agendamentos")
        expenses = fetch_rows(conn, "despesas")
        patients = fetch_rows(conn, "pacientes")
    finally:
        conn.close()

    transactions: list[dict[str, Any]] = []
    skipped_appointments = 0
    skipped_expenses = 0

    for row in appointments:
        tx = appointment_to_tx(row)
        if tx:
            transactions.append(tx)
        else:
            skipped_appointments += 1

    for row in expenses:
        tx = expense_to_tx(row)
        if tx:
            transactions.append(tx)
        else:
            skipped_expenses += 1

    transactions.sort(key=lambda tx: (tx["date"], tx["id"]), reverse=True)

    categories = [
        {"id": "fisio-atendimentos", "ico": "FZ", "name": "Atendimentos", "col": "#22c55e"},
        {"id": "fisio-negocio", "ico": "FZ", "name": "Negocio", "col": "#38bdf8"},
    ]

    return {
        "app": "Finanza",
        "version": "4.0.0",
        "source": {
            "name": "FisioSaaS",
            "sqlite": str(source),
            "exportedAt": datetime.now().isoformat(timespec="seconds"),
            "appointments": len(appointments),
            "expenses": len(expenses),
            "patients": len(patients),
            "skippedAppointments": skipped_appointments,
            "skippedExpenses": skipped_expenses,
        },
        "accounts": [
            {
                "id": DEFAULT_ACCOUNT_ID,
                "name": "FisioSaaS",
                "icon": "FZ",
                "type": "checking",
                "balance": 0,
                "yieldRate": 0,
                "yieldType": "manual",
                "yieldVal": 0,
                "calcBase": "du",
                "startDate": "",
                "note": "Conta criada pela migracao do FisioSaaS",
            }
        ],
        "transactions": transactions,
        "budgets": [],
        "goals": [],
        "categories": categories,
        "shopping": {"lists": [], "items": []},
        "settings": {
            "theme": "dark",
            "rates": {"dueItems": []},
            "widgetPrefs": {},
            "widgetOrder": [],
            "txView": "n",
            "activeList": None,
        },
    }


def write_backup(payload: dict[str, Any], output: Path) -> Path:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return output


def import_backup(payload: dict[str, Any], api_url: str, api_key: str) -> dict[str, Any]:
    clean_url = api_url.rstrip("/")
    request = urllib.request.Request(
        f"{clean_url}/api/import",
        data=json.dumps(payload).encode("utf-8"),
        method="PUT",
        headers={
            "content-type": "application/json",
            "x-api-key": api_key,
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        body = response.read().decode("utf-8")
        return json.loads(body) if body else {"success": True}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Converte dados financeiros do FisioSaaS para backup do Finanza.")
    parser.add_argument("--source", default=os.getenv("FISIOSAAS_DB", str(DEFAULT_SOURCE)), help="Caminho do agenda.db")
    parser.add_argument("--out", default="", help="Arquivo JSON de saida")
    parser.add_argument("--api-url", default=os.getenv("FINANZA_API_URL", ""), help="URL da API Finanza")
    parser.add_argument("--api-key", default=os.getenv("FINANZA_API_KEY", ""), help="API key do usuario Finanza")
    parser.add_argument("--import", dest="do_import", action="store_true", help="Envia o backup para /api/import")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = Path(args.source).expanduser().resolve()
    output = Path(args.out) if args.out else Path("backups") / f"fisiosaas_finanza_{now_slug()}.json"

    payload = build_backup(source)
    write_backup(payload, output)

    tx_count = len(payload["transactions"])
    print(f"Backup criado: {output}")
    print(f"Transacoes: {tx_count}")
    print(f"Agendamentos lidos: {payload['source']['appointments']}")
    print(f"Despesas lidas: {payload['source']['expenses']}")

    if args.do_import:
        if not args.api_url or not args.api_key:
            print("Erro: --import precisa de --api-url e --api-key, ou FINANZA_API_URL/FINANZA_API_KEY.", file=sys.stderr)
            return 2
        try:
            result = import_backup(payload, args.api_url, args.api_key)
        except urllib.error.HTTPError as exc:
            print(f"Erro HTTP ao importar: {exc.code} {exc.read().decode('utf-8', errors='replace')}", file=sys.stderr)
            return 3
        print("Importacao concluida:")
        print(json.dumps(result, ensure_ascii=False, indent=2))

    time.sleep(0.01)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
