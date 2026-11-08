# Configuração do Apotheosis Boss Scaling

Guia detalhado de cada campo de configuração.

## Onde fica

- **Arquivo**: `config/apothboss_scaling-server.toml` (gerado automaticamente na primeira execução).
- **Interface**: tecla **Ctrl+S** (configurável) ou comando `/apothboss_scaling`, com permissão de operador.
- Para **regenerar** com os padrões, apague o arquivo e reinicie o jogo.

## Como o cálculo funciona

O multiplicador final de cada atributo (vida, dano, armadura) é:

```
multiplicador final = tier × mod × jogadores
```

- **tier** — o multiplicador da seção `multipliers.<tier>` (baseado no World Tier do jogador mais próximo).
- **mod** — o fator da seção `mod_scaling` (baseado no mod de origem da entidade).
- **jogadores** — o fator da seção `player_scaling`, aplicado **somente a bosses** (não a mobs comuns).

---

## `[general]`

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `enabled` | bool | `true` | Interruptor geral. `false` desliga todo o scaling. |
| `scale_common_mobs` | bool | `true` | Escala monstros comuns (hostis que não são boss). |
| `scale_bosses` | bool | `true` | Escala bosses. |
| `max_health` | bool | `true` | Ativa/desativa o scaling de vida máxima. |
| `attack_damage` | bool | `true` | Ativa/desativa o scaling de dano de ataque. |
| `armor` | bool | `true` | Ativa/desativa o scaling de armadura. |
| `debug_log` | bool | `false` | Logs detalhados (entidade, tier e multiplicadores) no console. |
| `excluded_entities` | lista | `[]` | Entidades **excluídas** do scaling. Formato `"modid:entidade"` (ex.: `"minecraft:zombie"`). |
| `boss_entities` | lista | `[]` | Entidades tratadas **explicitamente como boss**. Formato `"modid:entidade"` (ex.: `"iceandfire:fire_dragon"`). |

---

## `[boss_detection]`

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `health_threshold` | double | `100.0` | Vida base máxima mínima (antes do scaling) para classificar uma entidade como **boss**. Entidades com vida base ≥ este valor são boss; abaixo disso, são mob comum (hostil). |

> Um mob é boss se: estiver na lista `boss_entities` **ou** tiver vida base ≥ `health_threshold`. Bosses são afetados pelo `player_scaling`; mobs comuns não.

---

## `[player_scaling]` (scaling por jogadores próximos)

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `enabled` | bool | `true` | Ativa/desativa o scaling por número de jogadores próximos. |
| `radius` | double | `100.0` | Raio (em blocos) para contar jogadores próximos ao mob. |
| `exponent` | double | `0.5` | Expoente da fórmula `multiplicador = jogadoresPróximos ^ expoente`. `0.5` = raiz quadrada (1 jogador → 1×, 4 → 2×, 9 → 3×). `1.0` = linear. `0.0` = desliga o efeito. |
| `dynamic` | bool | `true` | Se `true`, reavalia (~2s) conforme jogadores se aproximam/afastam do boss já escalado. Se `false`, congela a contagem no momento do spawn. |

> O `player_scaling` vale **apenas para bosses** (não afeta mobs comuns).

---

## `[limits]`

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `armor_cap` | double | `30.0` | **Teto da armadura produzida pelo nosso scaling.** A redução de dano satura em 30 (80%); acima disso não muda nada. O teto vale só sobre o nosso bônus — **nunca reduzimos** armadura que já existe (gear, Apotheosis, etc.). |

---

## `[mod_scaling]` (fator por mod)

O fator `mod` é multiplicado sobre o fator `tier`. A origem é o **namespace** do ID da entidade (a parte antes de `:`).

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `minecraft` | double | `1.0` | Fator para mobs **vanilla** (`minecraft:*`). Ex.: tier 2× no Pinnacle → 2× final. |
| `default` | double | `1.5` | Fator para mobs de **outros mods** (qualquer namespace não listado em `overrides`). Ex.: tier 2× → 3× final. |
| `apotheosis` | double | `0.7` | Fator **exclusivo** para bosses do Apotheosis (Invaders/Elites, detectados pelas tags internas). Ex.: tier 2× → 1.4× final. |
| `overrides` | lista | `[]` | Fator por **mod específico**, formato `"modid:multiplicador"`. Ex.: `["iceandfire:1.7", "twilightforest:1.4"]`. |

> Prioridade: se a entidade é boss do Apotheosis → `apotheosis`. Senão, se o namespace está em `overrides` → valor do override. Senão, se é `minecraft` → `minecraft`. Senão → `default`.

> Para achar o namespace exato de cada mod, use a aba **"Mods"** da interface (lista todos os mods que registram mobs).

---

## `[multipliers.<tier>]` (multiplicadores por tier)

Para cada tier (`haven`, `frontier`, `ascent`, `summit`, `pinnacle`):

| Campo | Tipo | Descrição |
|---|---|---|
| `max_health` | double | Multiplicador de vida máxima naquele tier. |
| `attack_damage` | double | Multiplicador de dano de ataque naquele tier. |
| `armor` | double | Multiplicador de armadura naquele tier. |

Padrões:

| Tier | max_health | attack_damage | armor |
|---|---|---|---|
| haven | 1.0 | 1.0 | 1.0 |
| frontier | 1.15 | 1.15 | 1.1 |
| ascent | 1.35 | 1.35 | 1.2 |
| summit | 1.6 | 1.6 | 1.3 |
| pinnacle | 2.0 | 2.0 | 1.5 |

---

## Exemplo de config completa (padrões)

```toml
[general]
enabled = true
scale_common_mobs = true
scale_bosses = true
max_health = true
attack_damage = true
armor = true
debug_log = false
excluded_entities = []
boss_entities = []

[boss_detection]
health_threshold = 100.0

[player_scaling]
enabled = true
radius = 100.0
exponent = 0.5
dynamic = true

[limits]
armor_cap = 30.0

[mod_scaling]
minecraft = 1.0
default = 1.5
apotheosis = 0.7
overrides = []

[multipliers.haven]
max_health = 1.0
attack_damage = 1.0
armor = 1.0

[multipliers.frontier]
max_health = 1.15
attack_damage = 1.15
armor = 1.1

[multipliers.ascent]
max_health = 1.35
attack_damage = 1.35
armor = 1.2

[multipliers.summit]
max_health = 1.6
attack_damage = 1.6
armor = 1.3

[multipliers.pinnacle]
max_health = 2.0
attack_damage = 2.0
armor = 1.5
```

---

## Observações

- **Armadura** é multiplicativa sobre a armadura existente. Como o Apotheosis já adiciona armadura flat aos monstros por tier, a conta compõe: `(base + flat do Apotheosis) × nosso multiplicador`, limitada pelo `armor_cap`.
- **Bosses do Apotheosis** são mobs vanilla transformados (não têm namespace próprio), identificados pelas tags internas `apoth.boss`/`apoth.miniboss`. Por isso têm o fator `apotheosis` separado.
- O mod é **server-side** no scaling; a interface é **client opcional** (quem tem o mod no cliente vê/edita; sem o mod no cliente, só a tela fica indisponível).
