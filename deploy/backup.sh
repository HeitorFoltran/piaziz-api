#!/usr/bin/env bash
# Backup do Postgres de produção, criptografado com age. Rodado pelo cron do host
# (ver deploy/README.md, passo 10). O dump nunca toca o disco sem criptografia.
set -euo pipefail

RETENCAO_DIAS=14
DESTINO=/var/backups/azizaid
CHAVE_PUBLICA=/etc/azizaid/backup.age.pub
CONFIG=/etc/azizaid/backup.env

PASTA_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$PASTA_SCRIPT/docker-compose.prod.yml"

RCLONE_REMOTE=""
HEALTHCHECK_URL=""
if [[ -f "$CONFIG" ]]; then
	# shellcheck source=/dev/null
	source "$CONFIG"
fi

if [[ ! -s "$CHAVE_PUBLICA" ]]; then
	echo "ERRO: chave pública do age não encontrada em $CHAVE_PUBLICA" >&2
	exit 1
fi

umask 077
mkdir -p "$DESTINO"

ARQUIVO="$DESTINO/azizaid-$(date +%Y-%m-%d-%H%M).dump.age"
TEMP="$ARQUIVO.tmp"
trap 'rm -f "$TEMP"' EXIT

echo "$(date -Is) iniciando backup em $ARQUIVO"

# pg_dump de dentro do container, para a versão sempre bater com a do servidor.
docker compose -f "$COMPOSE_FILE" exec -T db \
	sh -c 'pg_dump -U "$POSTGRES_USER" -d azizaid_hub -Fc' \
	| age -R "$CHAVE_PUBLICA" > "$TEMP"

# Só vira backup depois de completo: um .tmp parcial nunca parece válido.
mv "$TEMP" "$ARQUIVO"
echo "$(date -Is) backup local ok ($(du -h "$ARQUIVO" | cut -f1))"

find "$DESTINO" -maxdepth 1 -type f -name 'azizaid-*.dump.age' -mtime +"$RETENCAO_DIAS" -print -delete

if [[ -n "$RCLONE_REMOTE" ]]; then
	# Retenção no destino remoto fica na regra de ciclo de vida do provedor.
	rclone copy "$ARQUIVO" "$RCLONE_REMOTE"
	echo "$(date -Is) cópia remota ok ($RCLONE_REMOTE)"
else
	echo "AVISO: RCLONE_REMOTE vazio, backup ficou só local em $DESTINO" >&2
fi

if [[ -n "$HEALTHCHECK_URL" ]]; then
	curl -fsS -m 10 "$HEALTHCHECK_URL" > /dev/null
fi

echo "$(date -Is) backup concluído"
