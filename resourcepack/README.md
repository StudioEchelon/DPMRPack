# DPMR Resource Pack

Pack Minecraft (1.20.6+) pour **armes 3D / textures custom / sons**.

## Build (générer le zip)

Depuis la racine du projet:

```bash
bash scripts/build-resourcepack.sh
```

Le zip sera créé ici: `dist/dpmr-pack.zip`

À la fin du script, le **SHA1** du zip est affiché : copie-le dans `plugins/DiviserPourMieuxRegner/config.yml` → `resource-pack.sha1` (40 caractères hex, sans espaces).

## Mise en ligne et « les textures ne se mettent pas à jour »

Minecraft met en cache le resource pack. Pour qu’un **nouveau** `.zip` soit bien pris en compte :

1. **Héberge** le fichier (URL directe vers le zip, pas une page web).
2. Mets **`resource-pack.sha1`** à jour avec le hash du **nouveau** fichier (voir sortie du script `build-resourcepack.sh`, ou `shasum -a 1 dpmr-pack.zip`).
3. Sur le serveur : **`/ressourcepack`** relit `config.yml`, ajoute un paramètre anti-cache à l’URL et redemande le pack au client.
4. Si tu ne peux pas toucher au sha1 tout de suite : incrémente **`resource-pack.pack-revision`** dans la config (change l’URL côté client).

Sans sha1 correct, le client peut garder l’ancien pack ou refuser le téléchargement.

## Mode local : zip dans le dossier du plugin (sans Nginx)

Le plugin peut **servir le pack lui-même** en HTTP sur un port dédié. Tu copies le zip à côté du plugin, sans site web séparé.

1. Build : `bash scripts/build-resourcepack.sh` → récupère `dist/dpmr-pack.zip`.
2. Sur le serveur Minecraft, crée le dossier et copie le fichier :
   - `plugins/DiviserPourMieuxRegner/resource-pack/dpmr-pack.zip`
3. Dans `plugins/DiviserPourMieuxRegner/config.yml` :

```yaml
resource-pack:
  enabled: true
  source: local
  local:
    file: "resource-pack/dpmr-pack.zip"
    http-port: 8163
    http-path: "/dpmr-pack.zip"
    bind-address: "0.0.0.0"
    public-url: "http://IP_PUBLIQUE_DU_VPS:8163/dpmr-pack.zip"
```

4. **Pare-feu** : ouvre le port TCP **`8163`** (ou celui choisi dans `http-port`) vers Internet — les **joueurs** téléchargent depuis leur PC vers cette IP:port.
5. Redémarre le serveur ou **`/dpmr warworld reload`**. Le **SHA1** est calculé automatiquement ; inutile de remplir `resource-pack.sha1` en mode `local`.
6. Après avoir remplacé le zip sur disque : même reload, puis **`/ressourcepack`** (ou reconnexion) pour les joueurs.

**Note** : Minecraft exige toujours une **URL** ; ici c’est `public-url`, qui pointe vers le mini-serveur intégré au plugin, pas vers un fichier « magique » sans HTTP.

## Pompe / fusils à pompe (CustomModelData)

Chaque arme = **item vanilla** + **override** dans `assets/minecraft/models/item/`. Le plugin lit les valeurs dans `weapons.custom-model-data` (`WeaponManager` : si la clé manque ou vaut 0, pas de CMD → tu restes sur le modèle vanilla).

| Arme | Item vanilla | CMD défaut | Modèle DPMR |
|------|----------------|------------|-------------|
| CM_SHOTGUN | golden_hoe | 5003 | `shotgun_common` |
| EP_FUSIL_POMPE | diamond_hoe | 5002 | `epic_shotgun` |
| FUSIL_POMPE_RL | brush | 5011 | `shotgun_common` |
| FUSIL_POMPE_CROIX | iron_hoe | 5012 | `fusil_pompe_croix` → texture `epic_shotgun` |
| EP_POMPE_ACIDE | iron_shovel | 5008 | `pompe_acide` |
| LG_POMPE_DOUBLE | stone_hoe | 5009 | `pompe_double` |
| LG_POMPE_DOUBLE_CYBER | netherite_pickaxe | 5010 | `pompe_double_cyber` |
| GHOST_POMPE | netherite_shovel | 5013 | `ghost_pompe` |
| DIVISER_POUR_MIEUX_REGNER | netherite_hoe | 5014 | `diviser_pmr` |
| GHOST_DIVISER | netherite_hoe | 5015 | `ghost_diviser` |

Les PNG doivent être copiés dans le zip via `scripts/build-resourcepack.sh` (sources dans `assets/` à la racine du repo).

### Fichiers attendus dans `assets/` (noms fixes)

| Fichier | Arme / objet plugin |
|---------|----------------------|
| `shotgun_common.png` | `CM_SHOTGUN`, `FUSIL_POMPE_RL` (pinceau) |
| `epic_shotgun.png` | `EP_FUSIL_POMPE`, `FUSIL_POMPE_CROIX` (texture partagée) |
| `pompe_acide.png` | `EP_POMPE_ACIDE` |
| `pompe_double.png` | `LG_POMPE_DOUBLE` |
| `pompe_double_cyber.png` | `LG_POMPE_DOUBLE_CYBER` |
| `ghost_pompe.png` | `GHOST_POMPE` |
| `revolver.png` | `REVOLVER` |
| `revolver_flamme.png` | `LG_REVOLVER_FLAMME` |
| `revolver_cyber.png` | `EP_REVOLVER_CYBER` |
| `revolver_chataigne.png` | `CM_REVOLVER_CHATAIGNE` |
| `carabine_mk18.png` | `CARABINE_MK18` (repli : ancien `gun_2-*.png`) |
| `carabine_rare.png` | `CARABINE_RARE` |
| `jerrycan.png` | `JERRYCAN` |
| `bandage.png` | bandages (paper) |
| `radar.png` | radar (boussole) — optionnel : sinon le script utilise temporairement `revolver.png` |

## Textures incluses (test)

- **Carabine MK18** (arme `CARABINE_MK18`)
  - Base item: `minecraft:golden_pickaxe`
  - CustomModelData: `1001`
  - Texture: `assets/dpmr/textures/item/carabine_mk18.png`

- **Jerrican** (arme `JERRYCAN`)
  - Base item: `minecraft:bucket`
  - CustomModelData: `2001`
  - Texture: `assets/dpmr/textures/item/jerrycan.png`

- **Bandage** (consommable)
  - Base item: `minecraft:paper`
  - CustomModelData: `3001`
  - Texture: `assets/dpmr/textures/item/bandage.png`

- **Radar joueur** (outil)
  - Base item: `minecraft:compass`
  - CustomModelData: `4001`
  - Texture: `assets/dpmr/textures/item/radar.png`

## Notes

- Le plugin applique le `CustomModelData` via `config.yml`:
  - `weapons.custom-model-data.CARABINE_MK18: 1001`
  - `weapons.custom-model-data.JERRYCAN: 2001`
  - `bandage.custom-model-data: 3001`
  - `radar.custom-model-data: 4001`
- Les overrides Minecraft sont dans:
  - `assets/minecraft/models/item/golden_pickaxe.json`
  - `assets/minecraft/models/item/bucket.json`
  - `assets/minecraft/models/item/paper.json`
  - `assets/minecraft/models/item/compass.json`

