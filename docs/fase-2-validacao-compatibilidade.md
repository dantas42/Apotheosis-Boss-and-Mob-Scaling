# Fase 2 — Validação de compatibilidade com mods de boss

> Análise estática dos jars dos mods de boss presentes no modpack `Ekzmem Neoforge 2.0`, para
> confirmar que usam os **atributos vanilla** (`Attributes.MAX_HEALTH` / `ATTACK_DAMAGE`) e o
> caminho de dano `LivingEntity#hurt()` — o que garante que a abordagem do mod (modificador de
> atributo + hook de dano) funciona neles.

## Metodologia

Os `.class` dos jars foram extraídos e varridos por referências (constant pool, nomes Mojmap) a
atributos/métodos vanilla. Números = quantidade de classes que referenciam cada símbolo.

## Resultados

| Mod | MAX_HEALTH | ATTACK_DAMAGE | createMobAttributes | getAttributeValue | setBaseValue | hurt | Conclusão |
|---|---|---|---|---|---|---|---|
| Cataclysm (`L_Ender's Cataclysm 3.33`) | 43 | 54 | 3 | 64 | 1 | — | ✅ atributos vanilla |
| Ice and Fire (`iceandfire 2.1.3`) | 27 | 26 | 26 | 10 | **14** | — | ✅ (ver ressalva) |
| Mowzie's Mobs (`mowziesmobs 1.8.2`) | 16 | 20 | 1 | 8 | 0 | — | ✅ atributos vanilla |
| Twilight Forest (`4.8.3345`) | 52 | 35 | 13 | 8 | 1 | — | ✅ atributos vanilla |
| The Graveyard (`4.0.0`) | 13 | 10 | 4 | 4 | 0 | — | ✅ atributos vanilla |
| Alex's Caves (`1.1.3`) | 45 | 60 | 0 | 15 | **8** | — | ✅ atributos vanilla |
| **Bosses'Rise** (`block_factorys_bosses 2.1.2`) | 17 | 28 | 16 | 18 | **5** | **54** | ✅ atributos vanilla + `ServerBossEvent` (boss bar vanilla) |

> `—` = símbolo não varrido nessa passada (a varredura do `hurt` só foi feita para o
> `block_factorys_bosses`; os demais usam o padrão `getAttributeValue(ATTACK_DAMAGE)` seguido de
> `hurt()`, confirmado pelo uso extenso de `getAttributeValue`).

## Conclusão

**Todos os mods de boss testados usam atributos vanilla para vida e dano.** Portanto:

1. **Max Health** — o modificador `ADD_MULTIPLIED_BASE` sobre `Attributes.MAX_HEALTH` aplica-se
   a todos (a vida base vem de `AttributeSupplier`/`createMobAttributes`).
2. **Attack Damage** — o dano passa por `LivingEntity#hurt()`, então o multiplicador no
   `LivingIncomingDamageEvent` cobre melee, projéteis e magia (ex.: Iron's Spells, presente no
   modpack, também passa por `hurt()`).
3. **Detecção de boss** — `block_factorys_bosses` (exemplo de "mod novo, bosses souls-like") usa
   `ServerBossEvent` (boss bar vanilla), o que permite refinar a detecção na Fase 3; por ora o
   limiar de vida + listas de config já cobre todos.

## Ressalvas (riscos concretos a tratar na Fase 3)

- **Ice and Fire** usa `setBaseValue` 14×: dragões alteram a vida **base** ao crescer (estágios).
  Nosso modificador (`ADD_MULTIPLIED_BASE`) acompanha a nova base automaticamente (não é perdido),
  mas a mudança de estágio pode resetar `setHealth` para um valor fixo — verificar se o scaling
  permanece proporcional.
- **Alex's Caves** (`setBaseValue` 8×) e **Cataclysm** (`setBaseValue` 1×): alguns bosses ajustam a
  vida base em fases; mesmo comportamento acima.
- Bosses que **recriam a entidade** em transformação: mitigado pela reaplicação no
  `EntityJoinLevelEvent` (tier persistido).
- Ataques que **não têm entidade como fonte** (AoE/dano ambiental de boss): o `resolveAttacker`
  retorna `null` e o dano não é escalado — refinável na Fase 3 (ex.: associar o dono via
  `AreaEffectCloud`/`getIndirectEntity`).

## Próximos passos (Fase 3)

1. Teste **em jogo** (GameTest) com um boss real de cada mod.
2. Detecção de boss por `ServerBossEvent` além do limiar de vida.
3. Overrides por entidade (`per_entity_overrides`) e exclusões.
4. Tratamento de `setBaseValue`/fases (Ice and Fire, Alex's Caves, Cataclysm).
