# Apotheosis Boss Scaling

Mod complementar para **Minecraft 1.21.1 (NeoForge)** que integra o sistema de **World Tier do
Apotheosis** aos bosses e monstros de outros mods.

O Apotheosis já escala monstros por tier, mas **apenas defensivamente** (armadura, armor toughness,
redução de dano). Ele **não escala `max_health` nem `attack_damage`**, o que faz bosses de mods
externos ficarem fracos no tier final (Pinnacle). Este mod preenche essa lacuna com
**multiplicadores independentes por atributo e por tier**.

## Requisitos

- JDK 21 (o Gradle baixa automaticamente via `foojay-resolver`, mas ter um JDK 21 instalado ajuda).
- Apotheosis **8.x** (dependência obrigatória, resolvida via Modrinth Maven).

## Como compilar

```bash
./gradlew build
```

O jar gerado fica em `build/libs/apothboss_scaling-<versão>.jar`.

> Na primeira execução o Gradle baixa o NeoForge, o Minecraft e o Apotheosis (pode demorar).
> Use `./gradlew --refresh-dependencies` se faltar alguma dependência.

Executar o cliente/servidor de desenvolvimento:

```bash
./gradlew runClient
./gradlew runServer
```

Rodar os testes:

```bash
./gradlew test
```

## Como funciona

1. **Detecção do tier** (`tier.TierResolver`): lê o World Tier do jogador mais próximo via API
   pública do Apotheosis (`WorldTier.getTier`), no momento do spawn/join da entidade.
2. **Scaling de atributos** (`scaling.AttributeScaler`): aplica `AttributeModifier` idempotente
   (`addOrReplacePermanentModifier`) sobre `Attributes.MAX_HEALTH` e `Attributes.ARMOR`.
3. **Scaling de Attack Damage** (`scaling.DamageScaler`): multiplica o dano no
   `LivingIncomingDamageEvent`, cobrindo melee, projéteis e magia de forma uniforme.
4. **Persistência** (`attachment.ScalingAttachments`): guarda o tier aplicado e o multiplicador de
   dano em anexos (attachments) serializados e **reaplica de forma idempotente** no join da
   entidade — o scaling sobrevive a save/load, chunk load, teletransporte e troca de dimensão.
5. **Scaling por jogadores** (`ScalingEngine.playerMultiplier`): multiplicador extra de
   `jogadoresPróximos ^ expoente` (estilo ScalingMobs), combinado ao tier — vida e dano aumentam
   conforme mais jogadores lutam próximos ao boss, com **reavaliação dinâmica** (~2s).

O mod é **server-side** no scaling (atributos sincronizam automaticamente), e **cliente+servidor** para a
interface (abaixo).

## Interface (tela)

- Abra com **Ctrl+S** (configurável) ou o comando **`/apothboss_scaling`**.
- **Client opcional**: o mod funciona num servidor mesmo que o cliente **não** o tenha instalado (o scaling é
  server-side). Sem o mod no cliente, apenas a tela fica indisponível — os packets da tela são registrados como
  *optional*, então clientes sem o mod não são desconectados.
- Mostra o **tier atual**, os **multiplicadores por tier**, os **toggles**, o **limiar de boss**, as **listas**
  e as **entidades escaladas próximas**.
- **Edição in-game** (multiplicadores, toggles, limiar, listas) — **apenas para operadores** (permissão nível 2,
  mesmo critério do comando `set_world_tier` do Apotheosis). Para não-OP a tela fica em modo somente leitura.

## Configuração

O arquivo é gerado em `config/apothboss_scaling-server.toml` (server-side). Destaques:

- `general.enabled`, `scale_common_mobs`, `scale_bosses` — toggles gerais.
- `general.max_health`, `general.attack_damage`, `general.armor` — ativa/desativa cada atributo.
- `general.excluded_entities`, `general.boss_entities` — listas por entity id (`modid:entidade`).
- `boss_detection.health_threshold` — vida base mínima para classificar como boss.
- `multipliers.<tier>.max_health` / `.attack_damage` / `.armor` — multiplicadores por tier
  (`haven`, `frontier`, `ascent`, `summit`, `pinnacle`).
- `player_scaling.enabled`, `radius`, `exponent`, `dynamic` — scaling por jogadores próximos
  (`multiplicador = jogadoresPróximos ^ expoente`; padrão raio 100 e expoente 0.5 = raiz quadrada).
- `mod_scaling.minecraft` / `.default` / `.overrides` — fator extra por mod, aplicado sobre o
  multiplicador do tier (`final = tier × mod`). `minecraft` = mobs vanilla (padrão 1.0), `default` =
  outros mods (padrão 1.5), e `overrides` = lista `"modid:multiplicador"` por mod específico.

## Status

| Fase | Estado |
|---|---|
| Fase 0 — Investigação | ✅ concluída (`docs/fase-0-relatorio-tecnico.md`) |
| Fase 1 — Arquitetura | ✅ concluída (`docs/fase-1-arquitetura.md`) |
| Fase 2 — MVP | ✅ implementado (este código) |
| Fase 3 — Compatibilidade | pendente |
| Fase 4 — Balanceamento/presets | pendente |
