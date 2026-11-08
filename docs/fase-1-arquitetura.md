# Fase 1 — Arquitetura: Apotheosis Boss Scaling

> Minecraft 1.21.1 · NeoForge 21.1.264 · Apotheosis 8.9.0 (dependência obrigatória)

## 0. Decisões fechadas na Fase 0

1. **Dependência obrigatória** no Apotheosis — compilamos direto contra `WorldTier` e
   `Apoth.Attachments` (sem reflection).
2. **Dano com máxima compatibilidade** (magias, bosses, mobs, projéteis) — escalar dano via
   **hook de evento** (`LivingIncomingDamageEvent`), não apenas via atributo `ATTACK_DAMAGE`,
   para cobrir fontes de dano não-atributo (magia de mods como Iron's Spells, projéteis, etc.).
3. **Persistência** — persistir o *estado* do scaling em `AttachmentType` (serializado) e
   reaplicar modificadores de forma idempotente no `EntityJoinLevelEvent`.

---

## 1. Estrutura de pacotes

```
dev.<user>.apothbossscaling
├── ApotheosisBossScaling              # @Mod principal: registra config, attachments, handlers
├── config
│   ├── ScalingConfig                  # ModConfigSpec (TOML), server-side (COMMON)
│   ├── TierMultipliers                # record: tier -> multiplicadores por atributo
│   └── Presets                        # Balanced / Hard / Brutal / Nightmare (mapas de dados)
├── tier
│   └── TierResolver                   # única ponte com Apotheosis (WorldTier.getTier)
├── scaling
│   ├── AttributeScaler                # aplica/remove AttributeModifier por atributo (idempotente)
│   ├── DamageScaler                   # multiplica dano no LivingIncomingDamageEvent
│   ├── ScalingEngine                  # orquestra: classifica entidade -> resolve tier -> aplica
│   └── EntityClassifier               # boss vs mob comum (heurísticas + config)
├── attachment
│   └── ScalingAttachments             # SCALED_TIER / DAMAGE_MULTIPLIER (persistidos)
├── override
│   └── EntityOverrides                # exclusões + multiplicadores por entity type
└── event
    └── ScalingEventHandler            # FinalizeSpawnEvent + EntityJoinLevelEvent + LivingIncomingDamageEvent
```

---

## 2. Sistema de detecção do World Tier

Classe única: `tier.TierResolver`.

```java
// integração única com Apotheosis (API pública, sem reflection)
public static WorldTier tierOf(Player player) {
    return WorldTier.getTier(player); // dev.shadowsoffire.apotheosis.tiers.WorldTier
}
```

- O tier é **por jogador** (`Apoth.Attachments.WORLD_TIER`, default `HAVEN`).
- Para entidades, resolver o **jogador mais próximo** (`ServerLevel.getNearestPlayer(...)`),
  como o próprio Apotheosis faz em `ApothMobEvents.finalizeMobSpawns`.
- Se `getNearestPlayer` retornar `null` → **não escalar** (comportamento gracioso).
- Guarda de presença do mod (embora dependência obrigatória, mantemos por segurança):
  `ModList.get().isLoaded("apotheosis")`.

### Registro no mod

`ApotheosisBossScaling` declara a dependência no `neoforge.mods.toml`:

```toml
[[dependencies.apotheosisboss_scaling]]
    modId = "apotheosis"
    type = "required"
    versionRange = "[8.0.0,)"
```

---

## 3. Sistema de scaling

### 3.1 Atributos (health/armor/toughness/knockback/speed)

Aplicados como **modificadores permanentes idempotentes**, espelhando o `AttributeAugment` do
Apotheosis (`tiers/augments/AttributeAugment.java`):

- `AttributeInstance inst = entity.getAttribute(Attributes.MAX_HEALTH);` — se `null`, ignora.
- Criar `AttributeModifier` com `ResourceLocation` **determinístico** do nosso mod,
  ex.: `apothbossscaling:tier/<tier>/<attribute>`.
- `inst.addOrReplacePermanentModifier(modifier)` — reaplicar substitui (não acumula).
- Operação recomendada: `Operation.ADD_MULTIPLIED_BASE` com valor `(mult - 1.0)`, ou
  `ADD_VALUE` calculado `(base * mult - base)`. Para multiplicadores puros, usar
  `ADD_MULTIPLIED_BASE` (semântica clara de "multiplicar o valor base").
- Preservar a fração de vida ao mexer no max health (padrão Apotheosis):
  ```java
  float healthPct = mob.getHealth() / mob.getMaxHealth();
  ... aplicar ...
  mob.setHealth(healthPct * mob.getMaxHealth());
  ```

Atributos escaláveis (config): `MAX_HEALTH`, `ATTACK_DAMAGE`*, `ARMOR`, `ARMOR_TOUGHNESS`,
`KNOCKBACK_RESISTANCE`, `MOVEMENT_SPEED`, `REGENERATION`**.
> * `ATTACK_DAMAGE` como atributo é opcional/desligado por padrão (o dano é coberto pelo hook).
> ** `REGENERATION` não é atributo vanilla; fase posterior (efeito/ticks), desligado por padrão.

### 3.2 Dano (máxima compatibilidade)

Classe `scaling.DamageScaler`, hook em `LivingIncomingDamageEvent`
(`net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent`, disparado por
`CommonHooks.onEntityIncomingDamage` — confirmado no patch do `LivingEntity`).

- Na aplicação do scaling, gravar `DAMAGE_MULTIPLIER` (float) no attachment da entidade.
- No evento, resolver o **atacante raiz**: `source.getEntity()`; se for projétil/indireto,
  usar `source.getDirectEntity()` ou o dono do projétil (`Projectile.getOwner()`).
- Se o atacante tem `DAMAGE_MULTIPLIER != 1.0`, `event.setAmount(event.getAmount() * mult)`.
- **Cobre uniformemente**: melee, projéteis, magia, bosses, dano custom — tudo passa por
  `LivingEntity.hurt()`. Sem dupla contagem (multiplicamos em um único ponto).

> Vantagem: dano escalado no momento do hit → robusto a transformações/fases de bosses
> (desde que a nova entidade seja re-escalada no join).

### 3.3 Fluxo da `ScalingEngine`

```
FinalizeSpawnEvent (LOW)            -> resolve tier (nearest player) -> classifica -> aplica
EntityJoinLevelEvent (LOWEST)       -> se não escalada: aplica (nearest player)
                                       se já escalada: REaplica idempotente do tier persistido
                                       (cobre save/load, chunk load, teletransporte, dimensão)
LivingIncomingDamageEvent (HIGH)    -> DamageScaler (multiplicador do atacante)
```

---

## 4. Configuração

Formato: **ModConfigSpec (TOML)** — padrão recomendado pelo NeoForge para config de servidor
(`@AutoConfig`, tipo `COMMON`). Estrutura:

```toml
[general]
enabled = true
scale_common_mobs = true
scale_bosses = true
consider_spawn_tier = true        # tier do jogador mais próximo no nascimento/join

[attributes]
max_health = true
attack_damage = true
armor = true
armor_toughness = true
knockback_resistance = false
movement_speed = false
regeneration = false

[preset]
name = "balanced"                 # balanced | hard | brutal | nightmare | custom

[tiers.haven]                     # multiplicadores por tier (custom sobrescreve preset)
max_health = 1.0
attack_damage = 1.0
armor = 1.0
...

[tiers.pinnacle]
max_health = 4.0
attack_damage = 3.0
...

[excluded_entities]
values = ["modid:entity_a", "modid:entity_b"]

[per_entity_overrides]            # multiplicadores custom por entidade
"modid:boss_x" = { max_health = 2.0, attack_damage = 1.5 }

[dimensions]
enabled = true                    # scaling por dimensão
whitelist = false                 # false = blacklist
values = ["minecraft:the_end"]
```

- **Presets** (`Presets.java`) são apenas mapas estáticos `tier -> multiplicadores`.
  A lógica de scaling é independente deles (lê de `TierMultipliers` resolvido da config).
- A config é **server-side**; sincronizada automaticamente pelo NeoForge (COMMON).

---

## 5. Persistência

### Problema
Modificadores permanentes de atributo **não são persistidos automaticamente** para mobs no
NeoForge 1.21.1 (`AttributeInstance.save()/load()` existem, mas não há patch ligando ao save
de `LivingEntity`/`Mob`). O Apotheosis só guarda um booleano `TIER_AUGMENTS_APPLIED`, o que é
frágil nesse ponto.

### Solução (mais robusta que o Apotheosis)
- `AttachmentType` **persistido** (`serialize(...)`) guarda o estado do scaling:
  - `SCALED_TIER` (`WorldTier` ou ordinal) — tier aplicado.
  - `DAMAGE_MULTIPLIER` (`float`) — multiplicador de dano resolvido.
- No `EntityJoinLevelEvent`, se `SCALED_TIER` existe → **reaplicar** os modificadores exatos
  (via `addOrReplacePermanentModifier`, idempotente), sem precisar de jogador próximo.
- Resultado: scaling **sobrevive a save/load, chunk load, teletransporte e troca de dimensão**,
  mesmo sem persistência de modificadores.

### Sincronização servidor↔cliente
- Atributos (health/armor/...) sincronizam automaticamente (dados da entidade).
- `DAMAGE_MULTIPLIER` é só server-side (usado no evento de dano, que roda no servidor) — não
  precisa sincronizar. `SCALED_TIER` também só server-side.
- Mod 100% server-side; nenhum packet custom necessário.

---

## 6. Compatibilidade (com bosses de outros mods)

### Classificação (`EntityClassifier`)
Sem API universal de boss. Combinar:
1. `entity instanceof Monster` (mobs comuns).
2. Boss bar ativa (`ServerBossEvent` / `CustomServerBossEvent` via `NeoForge.EVENT_BUS`
   `EntityEvent` ou `getServerBossEvent()`).
3. Vida base alta (heurística configurável, ex. `getMaxHealth() >= limiar`).
4. Tags em `getPersistentData()` (`apoth.boss`, chaves comuns de outros mods).
5. **Listas de config** (`excluded_entities`, `per_entity_overrides`) — fonte de verdade final.

### Salvaguardas
- `getAttribute(...) == null` → ignora o atributo graciosamente.
- Nunca escalar `Player` / `FakePlayer`.
- Respeitar `scale_common_mobs` / `scale_bosses` e exclusões antes de aplicar.
- IDs de modificador em namespace próprio → **não conflita** com os augments do Apotheosis
  (`apotheosis:*`) nem com outros mods.
- Reaplicação idempotente → sem duplicação, mesmo com spawn+join+clone.

### Riscos (tratados nas Fases 3/4)
- Bosses que **recriam a entidade** em fases → mitigado pela reaplicação no `EntityJoinLevelEvent`.
- Bosses com **vida/dano próprios** (fora de atributos) → dano coberto pelo hook; vida pode
  precisar de override específico (Fase 3).
- `add_multiplied_total` e ordem de modificadores → preferir `ADD_MULTIPLIED_BASE` (menos
  sensível à ordem).

---

## 7. Testes

- **Unit** (JUnit, sem servidor): resolução de multiplicadores por tier, classificação de
  entidades (dados mockados), idempotência de `AttributeScaler` (não duplica modificador).
- **GameTest / integração** (Fase 2+): spawn de mob em tier configurado → verificar
  `getMaxHealth()` e `getAttributeValue(ATTACK_DAMAGE)`; salvar/recarregar mundo → scaling mantido.
- **Logs de debug**: `-Dapothboss_scaling.debug=true` (ou config) habilita logs de aplicação
  (entidade, tier, multiplicadores), espelhando o `DEBUG_MOBS` do Apotheosis.

---

## 8. Pontos de integração concretos (referências)

| Propósito | Classe/Método | Fonte |
|---|---|---|
| Tier do jogador | `WorldTier.getTier(Player)` | `dev/shadowsoffire/apotheosis/tiers/WorldTier.java` |
| Attachment do tier | `Apoth.Attachments.WORLD_TIER` | `dev/shadowsoffire/apotheosis/Apoth.java` |
| Modificador permanente | `AttributeInstance.addOrReplacePermanentModifier` | NeoForge 1.21.1 (vanilla) |
| Atributos vanilla | `Attributes.MAX_HEALTH`, `ATTACK_DAMAGE`, ... | `net.minecraft.world.entity.ai.attributes.Attributes` |
| Evento de spawn | `FinalizeSpawnEvent` | `net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent` |
| Evento de join | `EntityJoinLevelEvent` | `net.neoforged.neoforge.event.entity.EntityJoinLevelEvent` |
| Evento de dano | `LivingIncomingDamageEvent` | `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent` |
| Jogador mais próximo | `ServerLevel.getNearestPlayer` | vanilla |
| Referência (spawn mobs) | `ApothMobEvents.finalizeMobSpawns` | `dev/shadowsoffire/apotheosis/mobs/ApothMobEvents.java` |
| Referência (join mobs) | `AdventureEvents.applyMissedTierAugments` | `dev/shadowsoffire/apotheosis/AdventureEvents.java` |
