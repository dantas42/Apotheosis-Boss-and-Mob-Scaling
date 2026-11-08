# Fase 0 — Relatório Técnico: Apotheosis Boss Scaling

> Investigação do sistema World Tier do Apotheosis (Minecraft 1.21.1 / NeoForge)
> para viabilizar um mod de scaling de bosses e monstros de outros mods.

## 0. Versões confirmadas

| Item | Valor | Fonte |
|---|---|---|
| Minecraft | 1.21.1 | `minecraftinstance.json` |
| NeoForge | 21.1.264 | `--fml.neoForgeVersion 21.1.264` |
| Apotheosis | 8.9.0 | `Apotheosis-1.21.1-8.9.0.jar` |
| Libs Apotheosis | Placebo, Apothic Attributes, Apothic Enchanting, Apothic Spawners | modpack |
| Fonte analisada | `Shadows-of-Fire/Apotheosis`, branch `1.21` | GitHub |

---

## 1. Como o World Tier é implementado?

`dev.shadowsoffire.apotheosis.tiers.WorldTier` é um **enum**:

```java
public enum WorldTier implements StringRepresentable {
    HAVEN("haven"),
    FRONTIER("frontier"),
    ASCENT("ascent"),
    SUMMIT("summit"),
    PINNACLE("pinnacle");
    // CODEC, STREAM_CODEC, BY_ID, getTier, setTier, isUnlocked, ...
}
```

O tier é **por jogador**, guardado em um **attachment** do NeoForge:

```java
// dev.shadowsoffire.apotheosis.Apoth.Attachments
public static final AttachmentType<WorldTier> WORLD_TIER =
    R.attachment("world_tier", () -> WorldTier.HAVEN,
        b -> b.serialize(WorldTier.CODEC).copyOnDeath().copyHandler((t, holder, prov) -> t));
```

- Desbloqueio por advancements (`Apoth.Advancements.WORLD_TIER_*`).
- Ativação na GUI de seleção (`CTRL+T`, `client.WorldTierSelectScreen`).
- Comando: `/apotheosis set_world_tier <player> <tier>` (`commands.WorldTierCommand`).
- Sincronização client<->server via `net.WorldTierPayload`.

## 2. Como o tier atual é determinado?

- **Jogador**: `WorldTier.getTier(Player)` lê `player.getData(Attachments.WORLD_TIER)`.
  - Trata `FakePlayer` resolvendo o jogador real pelo UUID.
- **Monstro**: **não é global, nem por dimensão.** É resolvido o **jogador mais próximo no
  momento do spawn/join**:

```java
// mobs.ApothMobEvents.finalizeMobSpawns(FinalizeSpawnEvent)
Player player = e.getLevel().getNearestPlayer(e.getX(), e.getY(), e.getZ(), -1, false);
GenContext ctx = GenContext.forPlayerAtPos(rand, player, mob.blockPosition()); // WorldTier.getTier(player)
```

O `GenContext` (record `tiers.GenContext`) carrega: `RandomSource, WorldTier, float luck,
ResourceKey<Level> dimension, Holder<Biome> biome, Set<String> stages`.

## 3. Existe API pública para acessar o tier?

**Sim.** Todos públicos e estáveis:

- `WorldTier.getTier(Player)`
- `WorldTier.setTier(Player, WorldTier)`
- `WorldTier.values()`, `WorldTier.CODEC`, `WorldTier.BY_ID`, `WorldTier.mapCodec(...)`
- `Apoth.Attachments.WORLD_TIER` (`AttachmentType<WorldTier>`)
- `WorldTier.isUnlocked(Player, WorldTier)`

## 4. Como consultar o tier de maneira segura?

```java
WorldTier tier = WorldTier.getTier(player);
// ou
WorldTier tier = player.getData(Apoth.Attachments.WORLD_TIER);
```

Para monstros: resolver `ServerLevel.getNearestPlayer(...)` e aplicar `WorldTier.getTier`.
Guarda de mod carregado: `ModList.get().isLoaded("apotheosis")`. Usar **somente** API pública
(nunca reflection/classes internas), para resistir a atualizações do Apotheosis.

## 5. Eventos NeoForge apropriados para aplicar o scaling

Confirmados no código-fonte (NeoForge 21.1):

- `net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent`
  (Apotheosis usa em `ApothMobEvents.finalizeMobSpawns`, `EventPriority.LOW`).
- `net.neoforged.neoforge.event.entity.EntityJoinLevelEvent`
  (Apotheosis usa em `AdventureEvents.applyMissedTierAugments` — cobre chunk load,
  summon por comando, estrutura, mudança de dimensão).
- (Opcional) `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent`
  e `LivingDamageEvent` — para escalar dano no momento do hit, se preferir.

## 6. Como impedir multiplicação repetida de atributos?

Apotheosis usa dois mecanismos (espelhar ambos):

1. **Flag persistente** `Apoth.Attachments.TIER_AUGMENTS_APPLIED`
   (`AttachmentType<Boolean>`, default `false`, serializado via `Codec.BOOL`).
2. **Modificador determinístico**: `AttributeInstance.addOrReplacePermanentModifier(modifier)`
   com `ResourceLocation` fixo — reaplicar substitui, não acumula.

Preservação de vida ao mexer no max health (padrão Apotheosis):

```java
float healthPct = mob.getHealth() / mob.getMaxHealth();
// aplica modificadores...
mob.setHealth(healthPct * mob.getMaxHealth());
```

## 7. Como garantir scaling em bosses de outros mods?

- Usar **atributos vanilla** (`Attributes.MAX_HEALTH`, `ATTACK_DAMAGE`, `ARMOR`,
  `ARMOR_TOUGHNESS`, `KNOCKBACK_RESISTANCE`, `MOVEMENT_SPEED`) via `LivingEntity.getAttribute`.
  Se retornar `null`, ignorar graciosamente.
- Aplicar **também** no `EntityJoinLevelEvent` (muitos bosses não passam pelo spawn normal).
- Detectar "boss" genericamente: `instanceof Monster`, boss bar (`ServerBossEvent`),
  vida base alta + listas de config (inclusão/exclusão por entity type). Não há API universal.
- Server-side apenas; atributos sincronizam automaticamente.

## 8. Principais riscos técnicos

1. Tier **por jogador** → scaling "congelado" no jogador mais próximo no spawn/join.
2. Bosses com vida/dano **customizados** (sobrescrevem `setMaxHealth`/`setHealth`,
   sistemas próprios de dano) podem ignorar o modifier.
3. **Fases/transformações** que recriam a entidade ou resetam atributos perdem o modifier.
4. `getAttribute(...)` retorna `null` para atributos não declarados.
5. **Duplicação** em spawn+join+clone+dimensão (mitigada por flag + addOrReplacePermanentModifier).
6. Conflito de **ordem de modificadores** (`add_multiplied_total`) com outros mods.
7. Sincronização: barras de vida customizadas podem não refletir.
8. Sem jogador próximo → mob não escalado (comportamento seguro/gracioso).
9. Mudanças internas do Apotheosis → usar apenas API pública.

## 9. Arquitetura recomendada

```
dev.<user>.apothbossscaling
├── ApotheosisBossScaling       (mod principal)
├── config/
│   ├── ScalingConfig           (NeoForge ModConfigSpec / TOML, server-side)
│   └── Presets                 (Balanced / Hard / Brutal / Nightmare — só dados)
├── tier/
│   └── TierResolver            (integração com Apotheosis via WorldTier)
├── scaling/
│   ├── AttributeScaler         (aplica/remove AttributeModifier por atributo)
│   ├── ScalingEngine           (tier → multiplicadores → entidade)
│   └── EntityClassifier        (boss vs mob comum)
├── attachment/
│   └── ScalingAttachments      (flag persistente "already scaled")
├── override/
│   └── EntityOverrides         (exclusões e multiplicadores por entity type)
└── event/
    └── ScalingEventHandler     (FinalizeSpawnEvent + EntityJoinLevelEvent)
```

- **Config**: `ModConfigSpec` (TOML) — padrão NeoForge para servidor. Presets como mapas
  estáticos tier→multiplicadores; lógica de scaling independente dos presets.
- **Integração**: dependência soft no Apotheosis (compilar contra API pública + guarda de mod).
- **Multiplicadores**: `AttributeModifier` com `Operation.ADD_MULTIPLIED_BASE` (ou `ADD_VALUE`
  calculado), `ResourceLocation` determinístico, `addOrReplacePermanentModifier`
  (espelhando `tiers.augments.AttributeAugment`).

## 10. Viabilidade

**Viável.** API pública e estável para o tier; mecanismo de scaling vanilla e bem compreendido.

**Descoberta-chave**: o Apotheosis **já escala monstros por tier, mas apenas defensivamente**.
Os `tier_augments` padrão (`data/apotheosis/tier_augments/<tier>/*.json`) adicionam:

- `minecraft:generic.armor` — `add_value` 4/8/12/16 (frontier/ascent/summit/pinnacle)
- `minecraft:generic.armor_toughness` — `add_value` 20 (summit/pinnacle)
- `apothic_attributes:armor_pierce`, `apothic_attributes:prot_pierce`
- `damage_reduction` física/mágica (0.15 no pinnacle)

**Nunca** escala `max_health` nem `attack_damage`. É exatamente a lacuna que este mod deve
preencher (escalar vida e dano — e opcionalmente os demais — por multiplicadores por tier).

---

## Referências concretas (classes/métodos/arquivos)

- `dev/shadowsoffire/apotheosis/tiers/WorldTier.java` — enum e `getTier/setTier`.
- `dev/shadowsoffire/apotheosis/Apoth.java` → `Attachments.WORLD_TIER`,
  `Attachments.TIER_AUGMENTS_APPLIED`, `Attachments.DAMAGE_REDUCTIONS`.
- `dev/shadowsoffire/apotheosis/tiers/augments/TierAugment.java` — interface + `Target` (PLAYERS/MONSTERS).
- `dev/shadowsoffire/apotheosis/tiers/augments/AttributeAugment.java` — aplica `AttributeModifier`.
- `dev/shadowsoffire/apotheosis/tiers/augments/DamageReductionAugment.java` — redução de dano.
- `dev/shadowsoffire/apotheosis/tiers/augments/TierAugmentRegistry.java` — `getAugments(tier, target)`.
- `dev/shadowsoffire/apotheosis/mobs/ApothMobEvents.java` — `finalizeMobSpawns`, `tryAugmentations`.
- `dev/shadowsoffire/apotheosis/AdventureEvents.java` — `applyMissedTierAugments`.
- `dev/shadowsoffire/apotheosis/tiers/GenContext.java` — contexto de tier/luck/dimensão/bioma.
- `dev/shadowsoffire/apotheosis/commands/WorldTierCommand.java` — `/apotheosis set_world_tier`.
- `dev/shadowsoffire/apotheosis/net/WorldTierPayload.java` — sync de tier.
- `data/apotheosis/tier_augments/**/*.json` — datapack de augments por tier.
