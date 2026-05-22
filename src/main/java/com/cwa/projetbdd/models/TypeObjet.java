package com.cwa.projetbdd.models;

/**
 * Type d'objet cosmetique :
 *  - badge      : icone affichee a cote du nom (1 actif a la fois)
 *  - titre      : libelle affiche sous le nom (1 actif a la fois)
 *  - theme      : skin de l'interface
 *  - cosmetique : autres elements decoratifs (cadres, effets, etc.)
 */
public enum TypeObjet {
    badge,
    titre,
    theme,
    cosmetique
}
