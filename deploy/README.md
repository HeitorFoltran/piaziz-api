# Produção na VPS — runbook

Passo a passo para montar e operar a produção do AziZaid Hub numa VPS (Ubuntu, Hostinger). Os arquivos de configuração ficam nesta pasta.

Render, Supabase e Vercel são **homologação**. A produção não reaproveita nada de lá: banco, segredos e usuários são todos novos, e dado real nunca entra em homologação.

Onde cada coisa fica na VPS:

| Caminho | O quê |
|---|---|
| `/srv/azizaid/azizaid-hub-api` | clone deste repo, fixado numa tag |
| `/srv/azizaid/azizaid-hub-frontend` | clone do frontend, fixado numa tag |
| `/srv/azizaid/frontend` | build estático que o Caddy serve |
| `/etc/azizaid/` | segredos (`db.env`, `api.env`, `backup.env`, chave pública do backup) |
| `/var/backups/azizaid/` | backups locais criptografados |

Em todo o documento, `<dominio>` é o domínio de produção e `<usuario>` é o seu usuário na VPS.

---

## 0. Decidir antes de começar

- **Domínio**: criar um registro A apontando para o IP da VPS, e também um AAAA se ela tiver IPv6. Se o DNS for da Cloudflare, deixar em **DNS only** (nuvem cinza). O modo proxy muda a cadeia de proxies e quebra o rate limit por IP sem configuração extra.
- **Destino externo do backup**: qualquer destino que o `rclone` suporte (Backblaze B2, Google Drive, outro servidor etc.). Tem que ficar fora da Hostinger.
- **Gerenciador de senhas**: vai guardar a senha do dono do banco, a senha do app, o `JWT_SECRET` e a chave privada do backup.

## 1. Servidor

Como root, no primeiro acesso:

```bash
adduser <usuario>
usermod -aG sudo <usuario>
```

No **seu computador**, copie a chave SSH e teste o acesso:

```bash
ssh-copy-id <usuario>@<ip-da-vps>
ssh <usuario>@<ip-da-vps>        # tem que entrar sem pedir senha
```

Na VPS, desligue o login por senha e o login de root. Crie `/etc/ssh/sshd_config.d/10-azizaid.conf` com:

```
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
```

O prefixo `10-` importa. O sshd usa o primeiro valor que encontra, e algumas imagens trazem um `50-cloud-init.conf` com `PasswordAuthentication yes`.

```bash
sudo sshd -t && sudo systemctl reload ssh
```

> **Atenção:** antes de fechar a sessão atual, abra outro terminal e confirme que `ssh <usuario>@<ip-da-vps>` continua funcionando. Se você ficar trancado para fora, o painel da Hostinger tem um terminal pelo navegador.

Atualizações, fuso horário e firewall:

```bash
sudo apt update && sudo apt full-upgrade -y
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
sudo timedatectl set-timezone America/Sao_Paulo

sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp      # HTTP/3
sudo ufw enable
```

Se o firewall do painel da Hostinger estiver ativo, libere as mesmas portas lá também. Reinicie se o `full-upgrade` trouxe kernel novo.

## 2. Docker, Caddy e ferramentas

- **Docker**: instale pelo [guia oficial](https://docs.docker.com/engine/install/ubuntu/) e confira que `docker --version` mostra **28 ou mais nova**. Versões anteriores deixavam máquinas da mesma rede alcançarem portas publicadas em `127.0.0.1`. Depois, rode `sudo usermod -aG docker <usuario>` e reconecte. Quem está no grupo `docker` tem, na prática, acesso de root, então só o seu usuário deve estar nele.
- **Caddy**: instale pelo [repositório oficial](https://caddyserver.com/docs/install#debian-ubuntu-raspbian).
- **Resto**:

  ```bash
  sudo apt install -y git rsync age rclone apache2-utils
  ```

  O `apache2-utils` só é necessário pelo `htpasswd`, que gera o hash de senha do primeiro usuário.

## 3. Código

```bash
sudo install -d -o $USER -g $USER /srv/azizaid
cd /srv/azizaid
git clone https://github.com/HeitorFoltran/piaziz-api.git azizaid-hub-api
git clone https://github.com/HeitorFoltran/piaziz-web.git azizaid-hub-frontend
git -C azizaid-hub-api checkout <tag>
git -C azizaid-hub-frontend checkout <tag>

echo "alias dcp='docker compose -f /srv/azizaid/azizaid-hub-api/deploy/docker-compose.prod.yml'" >> ~/.bashrc
source ~/.bashrc
```

Produção roda sempre uma **tag**, nunca `main` direto. Veja como criar uma em "Deploy de versão nova". Se os repos forem privados, use uma deploy key somente leitura no lugar do HTTPS.

## 4. Segredos

```bash
sudo install -d -m 700 -o $USER -g $USER /etc/azizaid
openssl rand -base64 32     # senha do dono do banco
openssl rand -base64 32     # senha do app
openssl rand -base64 48     # JWT_SECRET
```

Todos os valores são **novos**. Nada vem da homologação nem do `.env` local. O JWT identifica o usuário só pelo id, então um `JWT_SECRET` repetido faria um token de homologação valer como o mesmo id em produção. Guarde os três no gerenciador de senhas.

`/etc/azizaid/db.env`:

```
POSTGRES_USER=azizaid_owner
POSTGRES_PASSWORD=<senha do dono>
```

`/etc/azizaid/api.env`:

```
DB_USERNAME=azizaid_app
DB_PASSWORD=<senha do app>
JWT_SECRET=<jwt secret>
FRONTEND_BASE_URL=https://<dominio>
CORS_ALLOWED_ORIGINS=https://<dominio>
```

```bash
chmod 600 /etc/azizaid/*.env
```

## 5. Banco (uma vez só)

```bash
cd /srv/azizaid/azizaid-hub-api
dcp up -d db
dcp ps                      # esperar o db ficar "healthy"
```

Aplique o schema e crie o role do app:

> **Atenção:** rode o `01_schema.sql` **só agora, com o banco vazio**. O arquivo começa com `DROP TABLE ... CASCADE`. Rodar de novo depois disso apaga a produção inteira.

```bash
dcp exec -T db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub -v ON_ERROR_STOP=1' < db/01_schema.sql
dcp exec -T db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub -v ON_ERROR_STOP=1' < deploy/criar-role-app.sql
```

**Nunca rode o `db/02_seed.sql` em produção.** Ele cria usuários fictícios com uma senha conhecida.

Defina a senha do app e cadastre os serviços. Confirme a lista de serviços com a equipe antes.

```bash
dcp exec db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub'
```

```sql
\password azizaid_app
-- digite a mesma senha do DB_PASSWORD em api.env
INSERT INTO servico (nome) VALUES ('Psicologia'), ('Assistência Social'), ('Jurídico'), ('Saúde');
\q
```

## 6. API

```bash
dcp up -d --build api       # primeiro build leva alguns minutos
dcp logs -f api             # esperar "Started ...", sair com Ctrl+C
curl -fsS http://127.0.0.1:8081/actuator/health     # {"status":"UP"}
```

## 7. Primeiro usuário (DEV)

Nenhuma rota da API cria DEV. O primeiro usuário entra direto pelo banco.

```bash
htpasswd -nBC 10 "" | tr -d ':\n'; echo
```

O comando pede a senha duas vezes, sem gravar no histórico, e imprime o hash (`$2y$10$...`). Depois:

```bash
dcp exec db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub'
```

```sql
INSERT INTO profissional (nome, cpf, email, senha_hash, role)
VALUES ('<seu nome>', '<cpf com máscara>', '<seu email>', '<hash>', 'DEV');
```

Crie os demais usuários (PADRAO, ESTAGIARIO) pela tela, logado como DEV. Uma conta por pessoa, sem conta compartilhada: o log de auditoria depende disso.

## 8. Frontend

```bash
cd /srv/azizaid/azizaid-hub-frontend
docker run --rm -u "$(id -u):$(id -g)" -v "$PWD":/app -w /app \
  -e npm_config_cache=/tmp/.npm -e VITE_API_BASE_URL=https://<dominio> \
  node:22-alpine sh -c 'npm ci && npm run build'
mkdir -p /srv/azizaid/frontend
rsync -a --delete dist/ /srv/azizaid/frontend/
```

O `VITE_API_BASE_URL` fica embutido no build. Se o domínio mudar, é preciso buildar de novo.

## 9. Caddy

```bash
sudo cp /srv/azizaid/azizaid-hub-api/deploy/Caddyfile.example /etc/caddy/Caddyfile
sudo sed -i 's/azizaid\.example\.com/<dominio>/' /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl reload caddy
```

O Caddy emite o certificado sozinho. Para isso, o DNS já precisa estar apontando e as portas 80/443 abertas. Abra `https://<dominio>` e entre com o usuário DEV.

## 10. Backup

No **seu computador**, não na VPS:

```bash
age-keygen -o azizaid-backup.key
```

Guarde `azizaid-backup.key` no gerenciador de senhas e numa cópia offline. **Sem esse arquivo, nenhum backup pode ser aberto.** A linha `# public key: age1...` dentro dele é a chave pública.

Na VPS:

```bash
echo 'age1...' > /etc/azizaid/backup.age.pub     # só a chave pública
sudo install -d -m 700 -o $USER -g $USER /var/backups/azizaid
rclone config                                    # criar o remote do destino externo
```

`/etc/azizaid/backup.env`:

```
RCLONE_REMOTE=<remote>:<bucket ou pasta>
HEALTHCHECK_URL=https://hc-ping.com/<uuid>      # opcional, avisa se o backup parar de rodar
```

Rode uma vez na mão:

```bash
/srv/azizaid/azizaid-hub-api/deploy/backup.sh && ls -lh /var/backups/azizaid/
```

Agende no cron com `sudo nano /etc/cron.d/azizaid-backup`:

```
30 3 * * * <usuario> /srv/azizaid/azizaid-hub-api/deploy/backup.sh >> /var/backups/azizaid/backup.log 2>&1
```

No destino externo, configure uma regra de ciclo de vida que apague arquivos com mais de N dias (sugestão: 30). O backup guarda inclusive dados que já foram apagados do sistema, então esse prazo precisa caber na política de retenção (LGPD).

### Teste de restauração: fazer agora

Neste momento o banco só tem o schema, os serviços e o DEV, sem nenhum dado de atendida. Por isso dá para testar no seu computador sem risco. Baixe um backup (`scp`) e rode:

```bash
age -d -i azizaid-backup.key azizaid-<data>.dump.age > teste.dump
docker run -d --name restore-teste -e POSTGRES_PASSWORD=teste postgres:16
until docker exec restore-teste pg_isready -U postgres; do sleep 1; done
docker cp teste.dump restore-teste:/tmp/teste.dump
docker exec restore-teste createdb -U postgres azizaid_hub
docker exec restore-teste pg_restore -U postgres -d azizaid_hub --no-owner --no-acl /tmp/teste.dump
docker exec restore-teste psql -U postgres -d azizaid_hub -c 'SELECT nome FROM servico' -c 'SELECT email, role FROM profissional'
docker rm -f restore-teste && rm teste.dump
```

Depois que houver dado real, repita o teste mais ou menos a cada 6 meses, **na VPS**: dado de atendida não deve ir para o seu computador. Copie a chave privada para a VPS só durante o teste, restaure num container descartável e apague a chave com `shred -u`.

### Restauração de verdade (VPS perdida)

1. Refaça os passos 1 a 4 numa VPS nova.
2. Suba só o `db` (`dcp up -d db`). **Não rode o `01_schema.sql`.**
3. Crie o role antes de restaurar, porque o dump traz os grants para ele: `CREATE ROLE azizaid_app LOGIN;` no psql.
4. Descriptografe o backup e restaure com `pg_restore -U azizaid_owner -d azizaid_hub --exit-on-error`, de dentro do container (mesmos `docker cp` e `exec` do teste acima).
5. Rode `\password azizaid_app` no psql.
6. Siga os passos 6, 8, 9 e 10.

## 11. Verificação depois do cutover

1. **Portas**, a partir do seu computador:

   ```bash
   nc -zvw3 <dominio> 443      # conecta
   nc -zvw3 <dominio> 8081     # tem que falhar
   nc -zvw3 <dominio> 5432     # tem que falhar
   ```

2. **Headers e actuator**: `curl -sI https://<dominio>` precisa mostrar `strict-transport-security` e `content-security-policy`. E `curl -s https://<dominio>/actuator/health` **não** pode devolver o JSON do Spring (tem que vir o HTML do frontend).
3. **IP real, spoof e fuso**:
   - Faça login no app. Depois, no seu computador, rode:

     ```bash
     curl -X POST https://<dominio>/api/auth/login -H 'Content-Type: application/json' \
       -H 'X-Forwarded-For: 1.2.3.4' -d '{"email":"x@x.com","senha":"x"}'
     ```

   - Consulte o banco:

     ```sql
     SELECT timestamp, email_tentado, sucesso, ip_address FROM auth_audit_log ORDER BY id DESC LIMIT 2;
     ```

   - As duas linhas precisam mostrar o seu IP público (`curl ifconfig.me`). O `1.2.3.4` não pode aparecer em nenhuma. O `timestamp` precisa bater com o horário de Brasília agora.
   - Se o domínio tiver AAAA, repita o teste com `curl -4` e com `curl -6`.
4. **Revogação**: faça login no frontend e rode `UPDATE profissional SET sessoes_revogadas_em = now() WHERE email = '<seu email>';`. Clique em qualquer coisa no app: ele tem que voltar para a tela de login.
5. **CSP**: navegue pelas telas principais e pelo formulário público com o console do navegador aberto. Não pode aparecer nenhuma violação de `Content-Security-Policy`.
6. **Link de preenchimento**: gere um link e confira que ele começa com `https://<dominio>/ficha-publica/`. Abra numa aba anônima: o formulário tem que carregar. Não precisa enviar.
7. **Backup**: no dia seguinte, confira que há um arquivo novo em `/var/backups/azizaid/` e no destino externo.

## Deploy de versão nova

1. **No seu computador**, com a `main` testada (CI verde, e homologação ok se ela continuar existindo):

   ```bash
   git checkout main && git pull
   git tag v2026.10.05 && git push origin v2026.10.05
   ```

   O nome da tag é a data do deploy (`vAAAA.MM.DD`). Um segundo deploy no mesmo dia vira `vAAAA.MM.DD.2`. Faça o mesmo no repo do frontend, se ele mudou.

2. **API**, na VPS:

   ```bash
   cd /srv/azizaid/azizaid-hub-api
   git describe --tags                     # tag atual
   git fetch --tags origin
   git diff --name-only <tag atual> <tag nova> -- db/alteracoes/ deploy/
   git checkout <tag nova>
   ```

   Se o `diff` mostrou algo:
   - **Script novo em `db/alteracoes/`**: rode `deploy/backup.sh` e depois o script, **antes** de subir o código:

     ```bash
     dcp exec -T db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub -v ON_ERROR_STOP=1' < db/alteracoes/<script>.sql
     ```

   - **`deploy/Caddyfile.example` mudou**: repita o passo 9.
   - **`deploy/docker-compose.prod.yml` mudou**: o `dcp up -d` abaixo aplica a mudança.

   ```bash
   dcp up -d --build api
   curl -fsS http://127.0.0.1:8081/actuator/health
   docker image prune -f
   ```

   A API fica fora do ar por uns 30 s durante o restart, e os contadores de rate limit zeram.

3. **Frontend**: `git fetch --tags origin && git checkout <tag nova>` no clone do frontend, depois repita o passo 8.

### Rollback

Faça `git checkout <tag anterior>` e rode os mesmos comandos. O banco **não volta junto**. Por isso os scripts de `db/alteracoes/` são sempre aditivos: só acrescentam coluna, tabela ou índice, e o código antigo ignora o que é novo. Nunca remova nem renomeie uma coluna na mesma versão que para de usá-la. Primeiro sai uma versão que não usa mais a coluna, e só num deploy seguinte outra que a remove.

## Comandos úteis

```bash
dcp ps                                                        # estado dos containers
dcp logs --tail 200 api                                       # logs da API
dcp exec db sh -c 'psql -U "$POSTGRES_USER" -d azizaid_hub'   # psql como dono
```

```sql
-- desativar uma conta (vale na hora, para todas as sessões abertas)
UPDATE profissional SET ativo = false WHERE email = '...';
-- derrubar as sessões de uma pessoa sem desativar a conta (ex.: suspeita de token vazado)
UPDATE profissional SET sessoes_revogadas_em = now() WHERE email = '...';
-- promover alguém a DEV (não existe rota na API para isso)
UPDATE profissional SET role = 'DEV' WHERE email = '...';
```
