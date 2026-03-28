package fr.dpmr.game;

public enum FireMode {
    HITSCAN,
    PROJECTILE_SNOWBALL,
    PROJECTILE_ARROW,
    PROJECTILE_EGG,
    PROJECTILE_LLAMA_SPIT,
    /** Châtaignes = petites explosions zone */
    PROJECTILE_CHESTNUT,
    /** 5 projectiles (pompe) */
    PROJECTILE_SHOTGUN_FIVE,
    /** Fusil a pompe 4 directions (avant/arriere/gauche/droite) */
    HITSCAN_CROSS,
    /** Essence: projectile qui laisse une zone inflammable */
    PROJECTILE_GASOLINE,
    /** Bombe avec upgrades rebond / salve */
    PROJECTILE_BOMB,
    /** Missile nucleaire (charge longue, zone visible) */
    NUCLEAR_STRIKE,
    /** Rayon sans degats — attire le joueur vers le point vise (grappin) */
    GRAPPLE_BEAM,
    /** Flacon jeté : zone au sol (soin / buff alliés ou debuff zone) */
    PROJECTILE_SERUM_ZONE,
    /** Projectile : soigne un joueur touché (pas le tireur) */
    PROJECTILE_HEAL_DART
}
