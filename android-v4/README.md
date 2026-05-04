# Finanza v4 Android

Base nativa do app Android v4.

## Estado atual

Entregas ja visiveis nesta base:

- Room local-first como fonte principal de dados.
- Sincronizacao opcional com a API do Finanza, incluindo sync em background.
- Backup local com exportacao v4 e importacao compativel com backup legado `4.x`.
- Widgets de tela inicial para lancamento rapido, lista de compras, saldo e vencimentos.
- Notificacao persistente com atalhos rapidos.
- Bloqueio local por biometria ou PIN do app.

## Arquitetura

- Kotlin + Jetpack Compose para a interface.
- Material 3 com tema próprio do Finanza.
- Room para cache/local-first.
- DataStore para preferências leves.
- WorkManager reservado para sincronização, backup e notificações recorrentes.
- Camadas separadas em `core`, `data`, `domain` e `ui`.

## Primeiro marco

Esta pasta nasce separada da pasta `android/` do Capacitor para manter a v3.9.1 estável enquanto a v4 é reescrita. O pacote usa `com.finanza.v4` e `applicationIdSuffix = ".debug"` no debug para poder instalar junto com o app antigo durante a migração.

## Build

Usando o wrapper já existente na pasta `android/`:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\android\gradlew.bat -p android-v4 :app:assembleDebug
```

Crie um `android-v4/local.properties` local apontando para o Android SDK, por exemplo:

```properties
sdk.dir=C\:\\Users\\jeffe\\AppData\\Local\\Android\\Sdk
```

Se quiser configurar `android/` e `android-v4/` de uma vez, rode na raiz do projeto:

```powershell
.\configure-android-sdk.ps1
```
