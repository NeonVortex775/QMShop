# QMShop

A shop whose screen is generated at runtime and pushed to the client through DragonCore. Nothing has
to be placed in `DragonCore/Gui/` to get started: one YAML file per shop describes the products, the
plugin builds the rows, the paging and the purchase panel from that.

If you want a hand-drawn layout instead, drop a DragonCore GUI file into `gui/` and the plugin will
fill it with the same data.

## Requirements

| Plugin | Needed | Why |
| --- | --- | --- |
| DragonCore 2.6.x | yes | renders the screen |
| Vault + Essentials | for money prices | balance lookup and withdrawal |
| PlayerPoints | for point prices | `type: points` |
| NeigeItems | no | `ni:` items |
| NBTAPI | no | NBT matching on requirements |

Paper 1.12.2, DragonCore 2.6.2.9, Java 8.

## Install

1. Put the jar into `plugins/` and start once. `config.yml`, `shops/` and `gui/` are generated;
   `shops/灵材.yml` is copied in as an example.
2. Textures for the built-in screen belong in the client pack under `resourcepacks/DragonCore/商城/`.
3. `/shop reload` after editing anything.

## Commands

| Command | Does | Permission |
| --- | --- | --- |
| `/shop` | open the default shop | `qingmingshop.use` |
| `/shop <id>` | open one shop | `qingmingshop.use` |
| `/shop open <player> <id>` | open it for someone else | `qingmingshop.open` |
| `/shop list` | ids and product counts | - |
| `/shop reload` | reload config, shops and templates | `qingmingshop.admin` |

Aliases: `/qmshop`, `/qmsshop`.

## Shop file

`shops/<id>.yml`. The file name is the shop id, so `/shop 灵材` reads `shops/灵材.yml`.

```yaml
title: "&6青冥 · 灵材商行"

layout:                     # optional, inherits config.yml gui: for anything left out
  width: 760
  rows: 4
  row-height: 48
  row-gap: 6
  panel-height: 92

items:
  - display:
      material: DIAMOND
      name: "&b一阶灵根"
      lore:
        - "&7蕴含天地灵气的基础灵材"
    amount: 1               # given per purchase
    permission: ""          # optional, hides the row from players without it
    commands: []            # optional extra console commands
    price:                  # every line is charged, all or nothing
      - type: item
        material: STONE
        amount: 2
      - type: points
        amount: 100
      - type: money
        amount: 500
```

`display` takes `material` (with optional `:data`), `ni`, `name`, `lore`, `glow`, `nbt`. A price line
with `type: item` takes the same block plus `amount`; `name` and `lore` on a requirement are match
conditions, not labels, so leave them out for plain materials. `data: "*"` (or `WOOL:*`) accepts any
data value, which is the way to ask for "any colour of this".

`type` also accepts `material` for items, `point` for points and `coin`, `coins` or `vault` for money.
A line with an unknown type is dropped with a console warning instead of silently becoming money, and
a product whose price lines all failed to parse is skipped at load.

Money is a decimal. `economy.money-decimals` decides how many places are kept in the price line and in
the balance check, so `500.5` is a valid price.

A requirement that only names a `material` matches the plain item: renamed copies and anything with
lore do not count, so players cannot hand in a customised piece of gear in place of the raw material.
On a server where every item carries lore from an attribute plugin that check gets in the way, and
`match.require-plain-item: false` turns it off.

Screen height is derived, not configured:

```
height = margin*2 + title-height + rows*row-height + (rows-1)*row-gap + 16 + panel-height
```

Products beyond `rows` are paged; the arrows appear on their own.

## Buying

One purchase is capped at a single stack: 64 items in total. A product that gives 16 per purchase can
be bought 4 times, one that gives 1 can be bought 64 times. `quantity.max` in the config is clamped to
that as well, and both the arrows and the quantity box stop at the limit instead of letting the charge
fail later.

The charge itself is all or nothing. Item requirements are counted first, then points and money are
taken, then the items are removed; if any step fails, everything already taken is given back and the
player is told which requirement was missing. A product without a usable price, or one whose icon
cannot be built (`ni` typo, NeigeItems missing), is refused before any money moves.

## Custom screen

`gui/*.yml` is a DragonCore GUI file. Template selection is `match: "<shop id>"` first, then
`match: "*"`, then the built-in screen. The plugin rewrites `match` before sending, so a wildcard
template does not become a global screen replacement.

The plugin fills components by name:

| Component | Content |
| --- | --- |
| `行N名` / `行N价` | product name / quantity and price |
| `行N图标` | product icon |
| `行N需求K` / `行N需求框K` | requirement icon and its frame |
| `行N选择字` | "select" or "selected" |
| `标题` / `页码` / `上一页` / `下一页` | header and paging |
| `面板xxx` | hidden while nothing is selected |
| `提示xxx` | shown while nothing is selected |

Placeholders usable in the template: `{index}`, `{row}`, `{qty}`, `{page}`, `{pages}`, `{w}`, `{h}`,
`{shop}`, `{title}`, `{rows}`, `{step}`, `{big}`.

Buttons talk to the plugin through packets, not through EasyGui component ids - ids change every
time the screen is refreshed, which is why a click that works once stops working after an update:

```yaml
actions:
  click: "方法.发包('QingMingShop', 'select', '{index}')"
```

| Action | Effect |
| --- | --- |
| `select <index>` | select a product |
| `qty 1` / `qty -1` / `qty set 8` | change the quantity |
| `page 1` / `page -1` | paging |
| `buy` / `cancel` | buy, clear the selection |
| `open <id>` | switch shop |
| `close` / `hint` | close, show the arrow hint |

Keep `rows` in the template equal to or below the shop's `layout.rows`. A template that draws more
rows makes the next page spill into the visible area.

## Configuration

`config.yml` holds the defaults every shop inherits:

| Key | Meaning |
| --- | --- |
| `default-shop` | id opened by `/shop` |
| `refresh-mode` | `reopen` closes and reopens the screen, `update` refreshes in place |
| `gui.custom-enabled` | use `gui/` templates at all |
| `gui.width` `rows` `row-height` `row-gap` `margin` `title-height` `panel-height` | built-in layout |
| `gui.max-requirements` | requirement icons per row before the rest is hidden |
| `gui.texture-prefix` | texture folder inside the resource pack |
| `icons.points` / `icons.money` | what the currency requirements look like |
| `economy.points-name` / `money-name` / `money-decimals` | names shown in messages |
| `quantity.min` / `max` / `step` / `big-step` / `default` | quantity selector, `max` cannot exceed 64 |
| `match.require-plain-item` | plain material requirements only match unnamed, lore-free items |
| `messages.*` | every string the plugin sends |
| `sounds.*` | 1.12 sound names, empty disables |

`refresh-mode: update` avoids the flicker but some clients lose their button hitboxes after an
in-place refresh, which is why `reopen` is the default.

## Build

```bash
mkdir -p lib   # spigot-api.jar, dragoncore.jar, vault.jar, playerpoints.jar
bash build.sh
# or: MC_SERVER=/path/to/server bash build.sh install
```

Output: `build/QingMingShop.jar`.
